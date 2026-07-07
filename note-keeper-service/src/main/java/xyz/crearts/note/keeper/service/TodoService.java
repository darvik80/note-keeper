package xyz.crearts.note.keeper.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.crearts.note.keeper.dto.TodoInput;
import xyz.crearts.note.keeper.exception.ResourceNotFoundException;
import xyz.crearts.note.keeper.mapper.AttachmentMapper;
import xyz.crearts.note.keeper.mapper.TodoMapper;
import xyz.crearts.note.keeper.model.Todo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TodoService {

    private final TodoMapper todoMapper;
    private final AttachmentMapper attachmentMapper;
    private final NotificationService notificationService;
    private final TagSyncService tagSyncService;
    private final ResourceAccessService resourceAccess;

    public TodoService(TodoMapper todoMapper, AttachmentMapper attachmentMapper,
                       NotificationService notificationService, TagSyncService tagSyncService,
                       ResourceAccessService resourceAccess) {
        this.todoMapper = todoMapper;
        this.attachmentMapper = attachmentMapper;
        this.notificationService = notificationService;
        this.tagSyncService = tagSyncService;
        this.resourceAccess = resourceAccess;
    }

    public List<Todo> findAll(Boolean completed, String tag, String priority,
                              Boolean isFavorite, Boolean isArchived, Boolean isDeleted, String ownerId) {
        return todoMapper.findAll(completed, tag, priority, isFavorite, isArchived, isDeleted, ownerId);
    }

    public List<Todo> findSharedWithMe(String userId) {
        return todoMapper.findSharedWithMe(userId);
    }

    public Todo findById(String id, String userId) {
        Todo todo = loadTodo(id);
        resourceAccess.requireTodoRead(todo, userId);
        return todo;
    }

    private Todo loadTodo(String id) {
        Todo todo = todoMapper.findById(id);
        if (todo == null) {
            throw new ResourceNotFoundException("Todo not found: " + id);
        }
        return todo;
    }

    @Transactional
    public Todo create(TodoInput input, String ownerId) {
        Todo todo = new Todo();
        todo.setId(UUID.randomUUID().toString());
        todo.setTitle(input.getTitle());
        todo.setDescription(input.getDescription());
        todo.setCompleted(input.getCompleted() != null && input.getCompleted());
        todo.setTags(input.getTags() != null ? input.getTags() : new ArrayList<>());
        todo.setPriority(input.getPriority() != null ? input.getPriority() : "medium");
        todo.setFavorite(input.getIsFavorite() != null && input.getIsFavorite());
        todo.setArchived(false);
        todo.setDeleted(false);
        todo.setOwnerId(ownerId);
        todo.setSharedWith("[]");
        todo.setDueDate(parseDate(input.getDueDate()));
        todo.setReminder(parseDate(input.getReminder()));
        todo.setNotificationChannels(input.getNotificationChannels());

        if (input.getLocation() != null) {
            todo.setLocation(convertToLocation(input.getLocation()));
        } else {
            todo.setLocation(new Todo.Location());
        }

        if (input.getSchedule() != null) {
            todo.setSchedule(convertToSchedule(input.getSchedule()));
        } else {
            Todo.Schedule schedule = new Todo.Schedule();
            schedule.setRepeat("none");
            todo.setSchedule(schedule);
        }

        LocalDateTime now = LocalDateTime.now();
        todo.setCreatedAt(now);
        todo.setUpdatedAt(now);

        todoMapper.insert(todo);

        if (input.getAttachments() != null && !input.getAttachments().isEmpty()) {
            saveAttachments(todo.getId(), "todo", input.getAttachments());
        }

        tagSyncService.addTags(ownerId, todo.getTags());
        notificationService.notifyTodoCreated(todo.getId(), ownerId);
        return findById(todo.getId(), ownerId);
    }

    @Transactional
    public Todo update(String id, TodoInput input, String userId) {
        Todo existing = loadTodo(id);
        resourceAccess.requireTodoOwner(existing, userId);
        List<String> oldTags = existing.getTags() != null ? new ArrayList<>(existing.getTags()) : new ArrayList<>();

        existing.setTitle(input.getTitle());
        existing.setDescription(input.getDescription());
        if (input.getCompleted() != null) existing.setCompleted(input.getCompleted());
        if (input.getTags() != null) existing.setTags(input.getTags());
        if (input.getPriority() != null) existing.setPriority(input.getPriority());
        if (input.getIsFavorite() != null) existing.setFavorite(input.getIsFavorite());
        existing.setDueDate(parseDate(input.getDueDate()));
        existing.setReminder(parseDate(input.getReminder()));
        if (input.getNotificationChannels() != null) {
            existing.setNotificationChannels(input.getNotificationChannels());
        }

        if (input.getLocation() != null) {
            existing.setLocation(convertToLocation(input.getLocation()));
        }
        if (input.getSchedule() != null) {
            existing.setSchedule(convertToSchedule(input.getSchedule()));
        }

        existing.setUpdatedAt(LocalDateTime.now());
        todoMapper.update(existing);

        if (input.getAttachments() != null) {
            attachmentMapper.deleteByParent(id, "todo");
            saveAttachments(id, "todo", input.getAttachments());
        }

        tagSyncService.updateTags(existing.getOwnerId(), oldTags, existing.getTags());
        notificationService.notifyTodoUpdated(id, existing.getOwnerId());
        return findById(id, userId);
    }

    @Transactional
    public void delete(String id, boolean permanent, String userId) {
        Todo todo = loadTodo(id);
        resourceAccess.requireTodoOwner(todo, userId);
        String ownerId = todo.getOwnerId();
        List<String> tags = todo.getTags();
        if (permanent) {
            attachmentMapper.deleteByParent(id, "todo");
            todoMapper.permanentDelete(id);
            tagSyncService.removeTagsIfUnused(ownerId, tags);
        } else {
            todoMapper.softDelete(id, LocalDateTime.now());
        }
        notificationService.notifyTodoDeleted(id, ownerId);
    }

    public Todo archive(String id, String userId) {
        Todo todo = loadTodo(id);
        resourceAccess.requireTodoOwner(todo, userId);
        todoMapper.archive(id);
        return findById(id, userId);
    }

    public Todo restore(String id, String userId) {
        Todo todo = loadTodo(id);
        resourceAccess.requireTodoOwner(todo, userId);
        todoMapper.restore(id);
        return findById(id, userId);
    }

    @Transactional
    public Todo shareWithUser(String todoId, String userIdToAdd, String currentOwnerId) {
        Todo todo = loadTodo(todoId);
        resourceAccess.requireTodoOwner(todo, currentOwnerId);

        List<String> sharedUsers = resourceAccess.parseSharedWith(todo.getSharedWith());
        if (!sharedUsers.contains(userIdToAdd)) {
            sharedUsers.add(userIdToAdd);
            String newSharedWith = toJsonArray(sharedUsers);
            todo.setSharedWith(newSharedWith);
            todoMapper.shareWithUser(todoId, newSharedWith);
        }

        return findById(todoId, currentOwnerId);
    }

    @Transactional
    public Todo unshareWithUser(String todoId, String userIdToRemove, String currentOwnerId) {
        Todo todo = loadTodo(todoId);
        resourceAccess.requireTodoOwner(todo, currentOwnerId);

        List<String> sharedUsers = resourceAccess.parseSharedWith(todo.getSharedWith());
        sharedUsers.remove(userIdToRemove);
        String newSharedWith = toJsonArray(sharedUsers);
        todo.setSharedWith(newSharedWith);
        todoMapper.shareWithUser(todoId, newSharedWith);

        return findById(todoId, currentOwnerId);
    }

    private String toJsonArray(List<String> list) {
        if (list.isEmpty()) {
            return "[]";
        }
        return "[" + list.stream()
                .map(s -> "\"" + s + "\"")
                .collect(Collectors.joining(",")) + "]";
    }

    private Todo.Location convertToLocation(Map<String, Object> map) {
        if (map == null) return new Todo.Location();
        Todo.Location location = new Todo.Location();
        Object lat = map.get("lat");
        Object lng = map.get("lng");
        Object address = map.get("address");
        if (lat instanceof Number) location.setLat(((Number) lat).doubleValue());
        if (lng instanceof Number) location.setLng(((Number) lng).doubleValue());
        if (address instanceof String) location.setAddress((String) address);
        return location;
    }

    private Todo.Schedule convertToSchedule(Map<String, Object> map) {
        if (map == null) return new Todo.Schedule();
        Todo.Schedule schedule = new Todo.Schedule();
        Object repeat = map.get("repeat");
        Object endDate = map.get("endDate");
        if (repeat instanceof String) schedule.setRepeat((String) repeat);
        if (endDate instanceof String) {
            String dateStr = (String) endDate;
            try {
                schedule.setEndDate(java.time.Instant.parse(dateStr).atZone(java.time.ZoneOffset.UTC).toLocalDateTime());
            } catch (Exception e) {
                try {
                    schedule.setEndDate(LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                } catch (Exception e2) {
                    try {
                        schedule.setEndDate(LocalDate.parse(dateStr).atStartOfDay());
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return schedule;
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            java.time.Instant instant = java.time.Instant.parse(dateStr);
            return LocalDateTime.ofInstant(instant, java.time.ZoneOffset.UTC);
        } catch (Exception e1) {
            try {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e2) {
                try {
                    return LocalDate.parse(dateStr).atStartOfDay();
                } catch (Exception e3) {
                    return null;
                }
            }
        }
    }

    private void saveAttachments(String parentId, String parentType, List<Map<String, Object>> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        for (Map<String, Object> attachmentData : attachments) {
            xyz.crearts.note.keeper.model.Attachment attachment = new xyz.crearts.note.keeper.model.Attachment();
            attachment.setId(UUID.randomUUID().toString());
            attachment.setParentId(parentId);
            attachment.setParentType(parentType);

            Object name = attachmentData.get("name");
            Object size = attachmentData.get("size");
            Object type = attachmentData.get("type");
            Object url = attachmentData.get("url");

            if (name instanceof String) attachment.setName((String) name);
            if (size instanceof Number) attachment.setSize(((Number) size).longValue());
            if (type instanceof String) attachment.setType((String) type);
            if (url instanceof String) attachment.setUrl((String) url);

            attachment.setUploadedAt(now);
            attachmentMapper.insert(attachment);
        }
    }
}

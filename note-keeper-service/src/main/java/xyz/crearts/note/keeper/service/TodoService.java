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

/**
 * Service for managing todos with owner support.
 * All todos are associated with an owner (user).
 */

@Service
public class TodoService {

    private final TodoMapper todoMapper;
    private final AttachmentMapper attachmentMapper;

    public TodoService(TodoMapper todoMapper, AttachmentMapper attachmentMapper) {
        this.todoMapper = todoMapper;
        this.attachmentMapper = attachmentMapper;
    }

    public List<Todo> findAll(Boolean completed, String tag, String priority,
                              Boolean isFavorite, Boolean isArchived, Boolean isDeleted, String ownerId) {
        // MyBatis автоматически загружает attachments через collection в resultMap
        return todoMapper.findAll(completed, tag, priority, isFavorite, isArchived, isDeleted, ownerId);
    }

    public List<Todo> findSharedWithMe(String userId) {
        // MyBatis автоматически загружает attachments через collection в resultMap
        return todoMapper.findSharedWithMe(userId);
    }

    public Todo findById(String id) {
        Todo todo = todoMapper.findById(id);
        if (todo == null) {
            throw new ResourceNotFoundException("Todo not found: " + id);
        }
        // MyBatis автоматически загружает attachments через collection в resultMap
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

        // Save attachments if provided
        if (input.getAttachments() != null && !input.getAttachments().isEmpty()) {
            saveAttachments(todo.getId(), "todo", input.getAttachments());
        }

        return findById(todo.getId());
    }

    @Transactional
    public Todo update(String id, TodoInput input) {
        Todo existing = findById(id);

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

        // Update attachments if provided
        if (input.getAttachments() != null) {
            // Delete existing attachments
            attachmentMapper.deleteByParent(id, "todo");
            // Save new attachments
            saveAttachments(id, "todo", input.getAttachments());
        }

        return findById(id);
    }

    @Transactional
    public void delete(String id, boolean permanent) {
        Todo todo = todoMapper.findById(id);
        if (todo == null) {
            throw new ResourceNotFoundException("Todo not found: " + id);
        }
        if (permanent) {
            attachmentMapper.deleteByParent(id, "todo");
            todoMapper.permanentDelete(id);
        } else {
            todoMapper.softDelete(id, LocalDateTime.now().toString());
        }
    }

    public Todo archive(String id) {
        findById(id);
        todoMapper.archive(id);
        return findById(id);
    }

    public Todo restore(String id) {
        findById(id);
        todoMapper.restore(id);
        return findById(id);
    }

    @Transactional
    public Todo shareWithUser(String todoId, String userIdToAdd, String currentOwnerId) {
        Todo todo = findById(todoId);
        System.out.println("[DEBUG] shareWithUser - todoId: " + todoId);
        System.out.println("[DEBUG] shareWithUser - todo.getOwnerId(): " + todo.getOwnerId());
        System.out.println("[DEBUG] shareWithUser - currentOwnerId (from JWT): " + currentOwnerId);
        if (todo.getOwnerId() == null) {
            throw new RuntimeException("Todo ownerId is null - data corruption");
        }
        if (currentOwnerId == null) {
            throw new RuntimeException("Current ownerId is null - authentication issue");
        }
        if (!todo.getOwnerId().equals(currentOwnerId)) {
            throw new RuntimeException("Only owner can share this todo (todo owner: " + todo.getOwnerId() + ", current user: " + currentOwnerId + ")");
        }

        List<String> sharedUsers = parseSharedWith(todo.getSharedWith());
        if (!sharedUsers.contains(userIdToAdd)) {
            sharedUsers.add(userIdToAdd);
            String newSharedWith = toJsonArray(sharedUsers);
            todo.setSharedWith(newSharedWith);
            todoMapper.shareWithUser(todoId, newSharedWith);
        }

        return findById(todoId);
    }

    @Transactional
    public Todo unshareWithUser(String todoId, String userIdToRemove, String currentOwnerId) {
        Todo todo = findById(todoId);
        if (!todo.getOwnerId().equals(currentOwnerId)) {
            throw new RuntimeException("Only owner can unshare this todo");
        }

        List<String> sharedUsers = parseSharedWith(todo.getSharedWith());
        sharedUsers.remove(userIdToRemove);
        String newSharedWith = toJsonArray(sharedUsers);
        todo.setSharedWith(newSharedWith);
        todoMapper.shareWithUser(todoId, newSharedWith);

        return findById(todoId);
    }

    private List<String> parseSharedWith(String sharedWith) {
        if (sharedWith == null || sharedWith.equals("[]")) {
            return new ArrayList<>();
        }
        try {
            String json = sharedWith.replace("[", "").replace("]", "").replace("\"", "");
            if (json.trim().isEmpty()) {
                return new ArrayList<>();
            }
            return Arrays.stream(json.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
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
                // Try ISO instant format first (e.g. "2026-06-19T00:00:00.000Z")
                schedule.setEndDate(java.time.Instant.parse(dateStr).atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            } catch (Exception e) {
                try {
                    // Fallback: ISO local date time without zone
                    schedule.setEndDate(LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                } catch (Exception e2) {
                    try {
                        // Fallback: date-only format (YYYY-MM-DD)
                        schedule.setEndDate(LocalDate.parse(dateStr).atStartOfDay());
                    } catch (Exception ignored) {
                        // Ignore invalid date
                    }
                }
            }
        }
        return schedule;
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            // Parse ISO UTC format (with 'Z') from frontend
            java.time.Instant instant = java.time.Instant.parse(dateStr);
            return LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
        } catch (Exception e1) {
            try {
                // Try ISO local date-time format
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e2) {
                try {
                    // Try "yyyy-MM-dd" format
                    return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (Exception e3) {
                    // Ignore invalid date
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

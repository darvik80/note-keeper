package xyz.crearts.note.keeper.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.crearts.note.keeper.dto.TodoInput;
import xyz.crearts.note.keeper.exception.ResourceNotFoundException;
import xyz.crearts.note.keeper.mapper.AttachmentMapper;
import xyz.crearts.note.keeper.mapper.TodoMapper;
import xyz.crearts.note.keeper.model.Attachment;
import xyz.crearts.note.keeper.model.Todo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        List<Todo> todos = todoMapper.findAll(completed, tag, priority, isFavorite, isArchived, isDeleted, ownerId);
        for (Todo todo : todos) {
            todo.setAttachments(attachmentMapper.findByParent(todo.getId(), "todo"));
        }
        return todos;
    }

    public List<Todo> findSharedWithMe(String userId) {
        List<Todo> todos = todoMapper.findSharedWithMe(userId);
        for (Todo todo : todos) {
            todo.setAttachments(attachmentMapper.findByParent(todo.getId(), "todo"));
        }
        return todos;
    }

    public Todo findById(String id) {
        Todo todo = todoMapper.findById(id);
        if (todo == null) {
            throw new ResourceNotFoundException("Todo not found: " + id);
        }
        todo.setAttachments(attachmentMapper.findByParent(todo.getId(), "todo"));
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
        todo.setDueDate(input.getDueDate());
        todo.setReminder(input.getReminder());

        if (input.getLocation() != null) {
            todo.setLocation(input.getLocation());
        } else {
            todo.setLocation(new Todo.Location());
        }

        if (input.getSchedule() != null) {
            todo.setSchedule(input.getSchedule());
        } else {
            Todo.Schedule schedule = new Todo.Schedule();
            schedule.setRepeat("none");
            todo.setSchedule(schedule);
        }

        LocalDateTime now = LocalDateTime.now();
        todo.setCreatedAt(now);
        todo.setUpdatedAt(now);

        todoMapper.insert(todo);

        if (input.getAttachments() != null) {
            for (Attachment att : input.getAttachments()) {
                att.setId(UUID.randomUUID().toString());
                att.setParentId(todo.getId());
                att.setParentType("todo");
                if (att.getUploadedAt() == null) att.setUploadedAt(now);
                attachmentMapper.insert(att);
            }
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
        existing.setDueDate(input.getDueDate());
        existing.setReminder(input.getReminder());

        if (input.getLocation() != null) {
            existing.setLocation(input.getLocation());
        }
        if (input.getSchedule() != null) {
            existing.setSchedule(input.getSchedule());
        }

        existing.setUpdatedAt(LocalDateTime.now());
        todoMapper.update(existing);

        if (input.getAttachments() != null) {
            attachmentMapper.deleteByParent(id, "todo");
            for (Attachment att : input.getAttachments()) {
                att.setId(att.getId() != null ? att.getId() : UUID.randomUUID().toString());
                att.setParentId(id);
                att.setParentType("todo");
                if (att.getUploadedAt() == null) att.setUploadedAt(LocalDateTime.now());
                attachmentMapper.insert(att);
            }
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
}

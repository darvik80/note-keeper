package xyz.crearts.note.keeper.service;

import org.springframework.stereotype.Service;
import xyz.crearts.note.keeper.exception.AccessDeniedException;
import xyz.crearts.note.keeper.model.Note;
import xyz.crearts.note.keeper.model.Todo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ResourceAccessService {

    public void requireNoteRead(Note note, String userId) {
        if (!canReadNote(note, userId)) {
            throw new AccessDeniedException("Not authorized to access this note");
        }
    }

    public void requireNoteOwner(Note note, String userId) {
        if (!isOwner(note.getOwnerId(), userId)) {
            throw new AccessDeniedException("Only the note owner can perform this action");
        }
    }

    public void requireTodoRead(Todo todo, String userId) {
        if (!canReadTodo(todo, userId)) {
            throw new AccessDeniedException("Not authorized to access this todo");
        }
    }

    public void requireTodoOwner(Todo todo, String userId) {
        if (!isOwner(todo.getOwnerId(), userId)) {
            throw new AccessDeniedException("Only the todo owner can perform this action");
        }
    }

    public boolean canReadNote(Note note, String userId) {
        return isOwner(note.getOwnerId(), userId) || isSharedWith(note.getSharedWith(), userId);
    }

    public boolean canReadTodo(Todo todo, String userId) {
        return isOwner(todo.getOwnerId(), userId) || isSharedWith(todo.getSharedWith(), userId);
    }

    public boolean isOwner(String ownerId, String userId) {
        return ownerId != null && ownerId.equals(userId);
    }

    public boolean isSharedWith(String sharedWith, String userId) {
        return parseSharedWith(sharedWith).contains(userId);
    }

    List<String> parseSharedWith(String sharedWith) {
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
}

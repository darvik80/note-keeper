package xyz.crearts.note.keeper.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import xyz.crearts.note.keeper.dto.TodoInput;
import xyz.crearts.note.keeper.model.Todo;
import xyz.crearts.note.keeper.service.TodoService;

import java.util.List;

/**
 * Todo controller with owner support.
 * Extracts owner ID from JWT token for all operations.
 */

@Slf4j
@RestController
@RequestMapping("/api/v1/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public List<Todo> getTodos(
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Boolean isFavorite,
            @RequestParam(required = false) Boolean isArchived,
            @RequestParam(required = false) Boolean isDeleted,
            @AuthenticationPrincipal String ownerId) {
        log.info("GET /api/v1/todos - ownerId: {}", ownerId);
        return todoService.findAll(completed, tag, priority, isFavorite, isArchived, isDeleted, ownerId);
    }

    @PostMapping
    public ResponseEntity<Todo> createTodo(@Valid @RequestBody TodoInput input,
                                           @AuthenticationPrincipal String ownerId) {
        log.info("POST /api/v1/todos - ownerId: {}, title: {}", ownerId, input.getTitle());
        Todo todo = todoService.create(input, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(todo);
    }

    @GetMapping("/{id}")
    public Todo getTodoById(@PathVariable String id) {
        return todoService.findById(id);
    }

    @PutMapping("/{id}")
    public Todo updateTodo(@PathVariable String id, @Valid @RequestBody TodoInput input) {
        return todoService.update(id, input);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTodo(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean permanent) {
        todoService.delete(id, permanent);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/archive")
    public Todo archiveTodo(@PathVariable String id) {
        return todoService.archive(id);
    }

    @PostMapping("/{id}/restore")
    public Todo restoreTodo(@PathVariable String id) {
        return todoService.restore(id);
    }

    @GetMapping("/shared-with-me")
    public List<Todo> getSharedWithMe(@AuthenticationPrincipal String ownerId) {
        log.info("GET /api/v1/todos/shared-with-me - ownerId: {}", ownerId);
        return todoService.findSharedWithMe(ownerId);
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<Todo> shareTodo(
            @PathVariable String id,
            @RequestParam String userId,
            @AuthenticationPrincipal String ownerId) {
        log.info("POST /api/v1/todos/{}/share - ownerId: {}, userId: {}", id, ownerId, userId);
        Todo todo = todoService.shareWithUser(id, userId, ownerId);
        return ResponseEntity.ok(todo);
    }

    @DeleteMapping("/{id}/share")
    public ResponseEntity<Todo> unshareTodo(
            @PathVariable String id,
            @RequestParam String userId,
            @AuthenticationPrincipal String ownerId) {
        log.info("DELETE /api/v1/todos/{}/share - ownerId: {}, userId: {}", id, ownerId, userId);
        Todo todo = todoService.unshareWithUser(id, userId, ownerId);
        return ResponseEntity.ok(todo);
    }
}

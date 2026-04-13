package xyz.crearts.note.keeper.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import xyz.crearts.note.keeper.dto.TodoInput;
import xyz.crearts.note.keeper.model.Todo;
import xyz.crearts.note.keeper.service.TodoService;

import java.util.List;

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
            @RequestParam(required = false) Boolean isDeleted) {
        return todoService.findAll(completed, tag, priority, isFavorite, isArchived, isDeleted);
    }

    @PostMapping
    public ResponseEntity<Todo> createTodo(@Valid @RequestBody TodoInput input) {
        Todo todo = todoService.create(input);
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
}

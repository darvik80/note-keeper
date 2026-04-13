package xyz.crearts.note.keeper.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import xyz.crearts.note.keeper.model.Attachment;
import xyz.crearts.note.keeper.model.Todo;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TodoInput {
    @NotBlank(message = "Title is required")
    private String title;
    private String description;
    private Boolean completed;
    private List<String> tags;
    private String priority;
    private Boolean isFavorite;
    private LocalDateTime dueDate;
    private LocalDateTime reminder;
    private Todo.Location location;
    private Todo.Schedule schedule;
    private List<Attachment> attachments;
}

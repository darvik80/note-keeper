package xyz.crearts.note.keeper.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class NoteTemplateInput {
    @NotBlank(message = "Name is required")
    private String name;
    @NotBlank(message = "Content is required")
    private String content;
    private List<String> tags;
    @NotBlank(message = "Category is required")
    private String category;
}

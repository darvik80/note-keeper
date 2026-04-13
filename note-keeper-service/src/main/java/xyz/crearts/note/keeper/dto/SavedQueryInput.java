package xyz.crearts.note.keeper.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import xyz.crearts.note.keeper.model.SavedQuery;

@Data
public class SavedQueryInput {
    @NotBlank(message = "Name is required")
    private String name;
    @NotBlank(message = "Query is required")
    private String query;
    private SavedQuery.Filters filters;
}

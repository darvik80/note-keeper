package dev.cogitor.note.keeper.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ApiTokenCreateRequest {
    @NotBlank
    private String name;
    private List<String> scopes;
    private String expiresAt;
}

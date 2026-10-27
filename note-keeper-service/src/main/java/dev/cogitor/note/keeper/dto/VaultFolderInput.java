package dev.cogitor.note.keeper.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VaultFolderInput {
    @NotBlank
    private String name;
    private String parentId;
}

package dev.cogitor.note.keeper.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VaultFolder {
    private String id;
    private String parentId;
    private String ownerId;
    private String name;
    private LocalDateTime updatedAt;
}

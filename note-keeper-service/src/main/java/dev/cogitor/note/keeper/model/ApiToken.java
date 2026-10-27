package dev.cogitor.note.keeper.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ApiToken {
    private String id;
    private String userId;
    private String name;
    private String tokenHash;
    private String prefix;
    private List<String> scopes = new ArrayList<>();
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
}

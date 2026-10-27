package dev.cogitor.note.keeper.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ApiTokenResponse {
    private String id;
    private String name;
    private String prefix;
    private List<String> scopes;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    /** Plaintext token — only present on create. */
    private String token;
}

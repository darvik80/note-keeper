package xyz.crearts.note.keeper.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * User credentials for password authentication.
 */
@Data
public class UserCredentials {
    private String userId;
    private String passwordHash;
    private String salt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

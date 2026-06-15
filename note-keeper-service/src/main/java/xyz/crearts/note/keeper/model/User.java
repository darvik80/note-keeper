package xyz.crearts.note.keeper.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * User model for authentication and ownership.
 */
@Data
public class User {
    private String id;
    private String email;
    private String name;
    @JsonProperty("picture")
    private String avatarUrl;
    private String provider; // 'local' or 'google'
    private String googleId;
    @JsonProperty("isActive")
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

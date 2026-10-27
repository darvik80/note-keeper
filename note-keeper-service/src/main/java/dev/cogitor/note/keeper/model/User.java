package dev.cogitor.note.keeper.model;

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
    private String provider; // 'local', 'google', or 'yandex'
    private String googleId;
    private String yandexId;
    @JsonProperty("isActive")
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

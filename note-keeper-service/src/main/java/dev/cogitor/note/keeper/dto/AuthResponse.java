package dev.cogitor.note.keeper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import dev.cogitor.note.keeper.model.User;

/**
 * Authentication response DTO.
 */
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private User user;
}

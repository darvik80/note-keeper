package xyz.crearts.note.keeper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import xyz.crearts.note.keeper.model.User;

/**
 * Authentication response DTO.
 */
@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private User user;
}

package xyz.crearts.note.keeper.dto;

import lombok.Data;

/**
 * Authentication request DTO.
 */
@Data
public class AuthRequest {
    private String email;
    private String password;
    private String name;
    private String googleId;
    private String googleToken;
}

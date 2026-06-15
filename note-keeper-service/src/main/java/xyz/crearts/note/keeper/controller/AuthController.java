package xyz.crearts.note.keeper.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyz.crearts.note.keeper.dto.AuthRequest;
import xyz.crearts.note.keeper.dto.AuthResponse;
import xyz.crearts.note.keeper.service.AuthService;

/**
 * Authentication controller for login and registration.
 * Google OAuth is handled by Spring Security OAuth2 Client (see SecurityConfig).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Register with email and password.
     */
    @PostMapping("/register")
    public AuthResponse register(@RequestBody AuthRequest request) {
        log.info("Register user with email: {}", request.getEmail());
        return authService.registerWithEmail(request);
    }

    /**
     * Login with email and password.
     */
    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        log.info("Login user with email: {}", request.getEmail());
        return authService.loginWithEmail(request);
    }
}

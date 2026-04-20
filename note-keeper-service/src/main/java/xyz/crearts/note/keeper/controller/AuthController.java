package xyz.crearts.note.keeper.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import xyz.crearts.note.keeper.dto.AuthRequest;
import xyz.crearts.note.keeper.dto.AuthResponse;
import xyz.crearts.note.keeper.service.AuthService;

/**
 * Authentication controller for login, registration, and Google OAuth.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

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

    /**
     * Login with Google OAuth token.
     */
    @PostMapping("/google")
    public AuthResponse loginWithGoogle(@RequestBody AuthRequest request) {
        log.info("Login with Google");
        // In production, validate googleToken and extract user info
        // For now, using mock data
        return authService.loginWithGoogle(
            request.getGoogleId(),
            request.getEmail(),
            request.getName(),
            null
        );
    }
}

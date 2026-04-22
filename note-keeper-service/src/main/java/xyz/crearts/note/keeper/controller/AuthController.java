package xyz.crearts.note.keeper.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import xyz.crearts.note.keeper.dto.AuthRequest;
import xyz.crearts.note.keeper.dto.AuthResponse;
import xyz.crearts.note.keeper.service.AuthService;

import java.util.Map;

/**
 * Authentication controller for login, registration, and Google OAuth.
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

    /**
     * Login with Google OAuth token.
     * Expects: { "credential": "google_id_token" }
     */
    @PostMapping("/google")
    public AuthResponse loginWithGoogle(@RequestBody Map<String, String> request) {
        String credential = request.get("credential");
        log.info("Login with Google OAuth token");
        
        // For now, extract info from client (in production, validate token with Google)
        // Client will decode JWT and send user info
        return authService.loginWithGoogle(
            request.get("googleId"),
            request.get("email"),
            request.get("name"),
            request.get("picture")
        );
    }
}

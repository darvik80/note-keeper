package xyz.crearts.note.keeper.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import xyz.crearts.note.keeper.dto.AuthRequest;
import xyz.crearts.note.keeper.dto.AuthResponse;
import xyz.crearts.note.keeper.service.AuthService;
import xyz.crearts.note.keeper.service.GoogleTokenVerifier;

import java.util.Map;

/**
 * Authentication controller for login, registration, and Google OAuth.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final GoogleTokenVerifier googleTokenVerifier;

    public AuthController(AuthService authService, GoogleTokenVerifier googleTokenVerifier) {
        this.authService = authService;
        this.googleTokenVerifier = googleTokenVerifier;
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
     * Server verifies the token signature using Google's public keys.
     */
    @PostMapping("/google")
    public AuthResponse loginWithGoogle(@RequestBody Map<String, String> request) {
        String credential = request.get("credential");
        if (credential == null || credential.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing 'credential' field");
        }

        log.info("Verifying Google OAuth token");
        GoogleTokenVerifier.GoogleUserInfo userInfo = googleTokenVerifier.verify(credential);
        if (userInfo == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google ID token");
        }

        log.info("Google OAuth verified for user: {}", userInfo.email());
        return authService.loginWithGoogle(
                userInfo.googleId(),
                userInfo.email(),
                userInfo.name(),
                userInfo.pictureUrl()
        );
    }
}

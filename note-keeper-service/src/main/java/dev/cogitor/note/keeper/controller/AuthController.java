package dev.cogitor.note.keeper.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import dev.cogitor.note.keeper.config.OAuth2SuccessHandler;
import dev.cogitor.note.keeper.dto.AuthRequest;
import dev.cogitor.note.keeper.dto.AuthResponse;
import dev.cogitor.note.keeper.model.User;
import dev.cogitor.note.keeper.service.AuthService;

import java.io.IOException;
import java.util.Set;

/**
 * Authentication controller for login, registration, and current user info.
 * Google OAuth is handled by Spring Security OAuth2 Client (see SecurityConfig).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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
     * Start Google/Yandex OAuth. Optional {@code redirect=notekeeper://oauth} for Android.
     */
    @GetMapping("/oauth/{provider}")
    public void startOAuth(@PathVariable String provider,
                           @RequestParam(required = false) String redirect,
                           HttpServletRequest request,
                           HttpServletResponse response) throws IOException {
        if (!Set.of("google", "yandex").contains(provider)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown OAuth provider");
            return;
        }
        if (redirect != null && OAuth2SuccessHandler.isAllowedMobileRedirect(redirect)) {
            request.getSession(true).setAttribute(OAuth2SuccessHandler.REDIRECT_SESSION_KEY, redirect);
        }
        response.sendRedirect(request.getContextPath() + "/oauth2/authorization/" + provider);
    }

    /**
     * Get current authenticated user info from JWT token.
     */
    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        User user = authService.validateToken(token);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(user);
    }
}

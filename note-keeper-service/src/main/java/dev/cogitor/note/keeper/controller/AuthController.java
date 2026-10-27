package dev.cogitor.note.keeper.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import dev.cogitor.note.keeper.config.OAuth2SuccessHandler;
import dev.cogitor.note.keeper.dto.AuthRequest;
import dev.cogitor.note.keeper.dto.AuthResponse;
import dev.cogitor.note.keeper.model.User;
import dev.cogitor.note.keeper.service.AuthService;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
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
     * Returns 302 Location — do not use {@code HttpServletResponse.sendRedirect} from
     * {@code @RestController} (Spring still tries to write a JSON body → 500).
     */
    @GetMapping("/oauth/{provider}")
    public ResponseEntity<Void> startOAuth(@PathVariable String provider,
                                           @RequestParam(required = false) String redirect,
                                           HttpServletRequest request) {
        if (!Set.of("google", "yandex").contains(provider)) {
            return ResponseEntity.badRequest().build();
        }
        HttpHeaders headers = new HttpHeaders();
        if (redirect != null && OAuth2SuccessHandler.isAllowedMobileRedirect(redirect)) {
            request.getSession(true).setAttribute(OAuth2SuccessHandler.REDIRECT_SESSION_KEY, redirect);
            ResponseCookie cookie = ResponseCookie.from(OAuth2SuccessHandler.REDIRECT_COOKIE, redirect)
                    .path("/")
                    .maxAge(Duration.ofMinutes(10))
                    .httpOnly(true)
                    .secure("https".equalsIgnoreCase(request.getScheme())
                            || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto")))
                    .sameSite("Lax")
                    .build();
            headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        String target = request.getContextPath() + "/oauth2/authorization/" + provider;
        headers.setLocation(URI.create(target));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    /**
     * Start OAuth to attach Google/Yandex to the current user (JWT required).
     */
    @PostMapping("/link/{provider}")
    public ResponseEntity<Map<String, String>> startLink(@PathVariable String provider,
                                                         @RequestHeader("Authorization") String authHeader,
                                                         HttpServletRequest request) {
        if (!Set.of("google", "yandex").contains(provider)) {
            return ResponseEntity.badRequest().build();
        }
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        User user = authService.validateToken(token);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        request.getSession(true).setAttribute(OAuth2SuccessHandler.LINK_USER_SESSION_KEY, user.getId());
        ResponseCookie cookie = ResponseCookie.from(OAuth2SuccessHandler.LINK_USER_COOKIE, user.getId())
                .path("/")
                .maxAge(Duration.ofMinutes(10))
                .httpOnly(true)
                .secure("https".equalsIgnoreCase(request.getScheme())
                        || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto")))
                .sameSite("Lax")
                .build();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
        String url = request.getContextPath() + "/oauth2/authorization/" + provider;
        return ResponseEntity.ok().headers(headers).body(Map.of("url", url));
    }

    @DeleteMapping("/link/{provider}")
    public ResponseEntity<User> unlink(@PathVariable String provider,
                                       @RequestHeader("Authorization") String authHeader) {
        if (!Set.of("google", "yandex").contains(provider)) {
            return ResponseEntity.badRequest().build();
        }
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        User user = authService.validateToken(token);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(authService.unlinkOAuth(user.getId(), provider));
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

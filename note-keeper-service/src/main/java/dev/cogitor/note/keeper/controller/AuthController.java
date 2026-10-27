package dev.cogitor.note.keeper.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import dev.cogitor.note.keeper.config.OAuth2SuccessHandler;
import dev.cogitor.note.keeper.dto.AuthRequest;
import dev.cogitor.note.keeper.dto.AuthResponse;
import dev.cogitor.note.keeper.dto.OAuthExchangeRequest;
import dev.cogitor.note.keeper.model.User;
import dev.cogitor.note.keeper.service.AuthService;
import dev.cogitor.note.keeper.service.OAuthCodeService;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final OAuthCodeService oauthCodeService;

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
                                           @RequestParam(required = false) String state,
                                           HttpServletRequest request) {
        if (!Set.of("google", "yandex").contains(provider)) {
            return ResponseEntity.badRequest().build();
        }
        HttpHeaders headers = new HttpHeaders();
        if (redirect != null && OAuth2SuccessHandler.isAllowedMobileRedirect(redirect)) {
            request.getSession(true).setAttribute(OAuth2SuccessHandler.REDIRECT_SESSION_KEY, redirect);
            boolean secure = "https".equalsIgnoreCase(request.getScheme())
                    || "https".equalsIgnoreCase(firstHop(request.getHeader("X-Forwarded-Proto")));
            ResponseCookie cookie = ResponseCookie.from(OAuth2SuccessHandler.REDIRECT_COOKIE, redirect)
                    .path("/")
                    .maxAge(Duration.ofMinutes(10))
                    .httpOnly(true)
                    .secure(secure)
                    .sameSite(secure ? "None" : "Lax")
                    .build();
            headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
            if (OAuth2SuccessHandler.isAllowedState(state)) {
                request.getSession(true).setAttribute(OAuth2SuccessHandler.STATE_SESSION_KEY, state);
                ResponseCookie stateCookie = ResponseCookie.from(OAuth2SuccessHandler.STATE_COOKIE, state)
                        .path("/")
                        .maxAge(Duration.ofMinutes(10))
                        .httpOnly(true)
                        .secure(secure)
                        .sameSite(secure ? "None" : "Lax")
                        .build();
                headers.add(HttpHeaders.SET_COOKIE, stateCookie.toString());
            }
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
     * Android OAuth landing page. Custom Tabs stay on https, then jump into the app via intent://.
     * Query carries a one-time code, never the JWT.
     */
    @GetMapping(value = "/oauth/app", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> androidOAuthBridge(@RequestParam String code,
                                                     @RequestParam(required = false) String state) {
        if (!OAuth2SuccessHandler.isAllowedCode(code)
                || (state != null && !state.isBlank() && !OAuth2SuccessHandler.isAllowedState(state))) {
            return ResponseEntity.badRequest().build();
        }
        String q = "code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);
        if (OAuth2SuccessHandler.isAllowedState(state)) {
            q += "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
        }
        String deep = OAuth2SuccessHandler.ANDROID_REDIRECT + "?" + q;
        String intent = "intent://oauth?" + q
                + "#Intent;scheme=notekeeper;package=" + OAuth2SuccessHandler.ANDROID_PACKAGE
                + ";end";
        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1">
                  <meta http-equiv="refresh" content="0;url=%s">
                  <title>NoteKeeper</title>
                </head>
                <body style="background:#0B0E11;color:#E9EDEF;font-family:sans-serif;padding:48px 24px">
                  <p>Opening NoteKeeper…</p>
                  <p><a href="%s" style="color:#E8A33D">Open the app</a></p>
                  <script>location.replace(%s);</script>
                </body>
                </html>
                """.formatted(htmlAttr(deep), htmlAttr(intent), jsString(intent));
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/html;charset=UTF-8")).body(html);
    }

    /**
     * Swap a one-time OAuth code for a JWT. Bound to the app-generated state.
     */
    @PostMapping("/oauth/exchange")
    public ResponseEntity<AuthResponse> exchangeOAuth(@RequestBody OAuthExchangeRequest request) {
        String jwt = oauthCodeService.consume(request.getCode(), request.getState());
        if (jwt == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = authService.validateToken(jwt);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new AuthResponse(jwt, user));
    }

    private static String firstHop(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        int comma = header.indexOf(',');
        return (comma < 0 ? header : header.substring(0, comma)).trim();
    }

    private static String htmlAttr(String value) {
        return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;");
    }

    private static String jsString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("</", "<\\/") + "\"";
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

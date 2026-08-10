package xyz.crearts.note.keeper.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import xyz.crearts.note.keeper.dto.AuthResponse;
import xyz.crearts.note.keeper.service.AuthService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Handles successful Google OAuth2 login by generating a JWT token
 * and redirecting the user back to the frontend with the token.
 */
@Component
@ConditionalOnProperty(name = "spring.security.oauth2.client.registration.google.client-id")
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private final AuthService authService;
    private final String publicBaseUrl;

    public OAuth2SuccessHandler(AuthService authService,
                                @Value("${app.public-base-url:}") String publicBaseUrl) {
        this.authService = authService;
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        log.info("Google OAuth2 success for user: {}", email);

        AuthResponse authResponse = authService.loginWithGoogle(googleId, email, name, picture);
        String token = authResponse.getToken();

        String baseUrl = resolveBaseUrl(request);
        String redirectUrl = baseUrl + "/#/login?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        log.info("OAuth2 redirect URL: {}/#/login?token=***", baseUrl);
        response.sendRedirect(redirectUrl);
    }

    /**
     * Public URL after Google login. Synology Reverse Proxy often sends
     * {@code X-Forwarded-Proto: https} + port {@code 80} → {@code https://host:80}.
     */
    String resolveBaseUrl(HttpServletRequest request) {
        if (!publicBaseUrl.isBlank()) {
            return publicBaseUrl;
        }

        String scheme = firstHop(request.getHeader("X-Forwarded-Proto"));
        if (scheme == null || scheme.isBlank()) {
            scheme = request.getScheme();
        }
        scheme = scheme.toLowerCase(Locale.ROOT);

        String hostHeader = firstHop(request.getHeader("X-Forwarded-Host"));
        String host;
        Integer port = null;

        if (hostHeader != null && !hostHeader.isBlank()) {
            int colon = hostHeader.lastIndexOf(':');
            if (colon > 0 && hostHeader.indexOf(']') < 0) {
                host = hostHeader.substring(0, colon);
                port = parsePort(hostHeader.substring(colon + 1));
            } else {
                host = hostHeader;
            }
        } else {
            host = request.getServerName();
            port = request.getServerPort();
        }

        if (port == null) {
            port = parsePort(firstHop(request.getHeader("X-Forwarded-Port")));
        }
        if (port == null) {
            port = request.getServerPort();
        }

        if (port != null && !isImplicitPublicPort(scheme, port)) {
            return scheme + "://" + host + ":" + port;
        }
        return scheme + "://" + host;
    }

    /** https+80 is Synology RP bug; browsers treat https default as 443. */
    static boolean isImplicitPublicPort(String scheme, int port) {
        if ("https".equals(scheme)) {
            return port == 443 || port == 80;
        }
        return "http".equals(scheme) && port == 80;
    }

    private static String firstHop(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        int comma = header.indexOf(',');
        return (comma < 0 ? header : header.substring(0, comma)).trim();
    }

    private static Integer parsePort(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

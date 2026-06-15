package xyz.crearts.note.keeper.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import xyz.crearts.note.keeper.dto.AuthResponse;
import xyz.crearts.note.keeper.service.AuthService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Handles successful Google OAuth2 login by generating a JWT token
 * and redirecting the user back to the frontend with the token.
 */
@Component
@ConditionalOnProperty(name = "spring.security.oauth2.client.registration.google.client-id")
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private final AuthService authService;

    public OAuth2SuccessHandler(AuthService authService) {
        this.authService = authService;
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

        // Create or update user and generate JWT
        AuthResponse authResponse = authService.loginWithGoogle(googleId, email, name, picture);
        String token = authResponse.getToken();

        // Build absolute redirect URL respecting reverse proxy headers
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme == null || scheme.isEmpty()) {
            scheme = request.getScheme();
        }
        // Force HTTPS when behind reverse proxy on non-standard port
        if ("http".equalsIgnoreCase(scheme) && request.getServerPort() != 8080) {
            scheme = "https";
        }

        String host = request.getHeader("X-Forwarded-Host");
        if (host == null || host.isEmpty()) {
            host = request.getServerName();
            int port = request.getServerPort();
            if (("https".equalsIgnoreCase(scheme) && port != 443)
                    || ("http".equalsIgnoreCase(scheme) && port != 80)) {
                host = host + ":" + port;
            }
        }

        String baseUrl = scheme + "://" + host;
        String redirectUrl = baseUrl + "/#/login?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        log.info("OAuth2 redirect URL: {}", redirectUrl);
        response.sendRedirect(redirectUrl);
    }
}

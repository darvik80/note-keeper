package dev.cogitor.note.keeper.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import dev.cogitor.note.keeper.dto.AuthResponse;
import dev.cogitor.note.keeper.service.AuthService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;

/**
 * Handles successful Google/Yandex OAuth2 login by generating a JWT
 * and redirecting back to the web app or Android custom scheme.
 */
@Component
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${spring.security.oauth2.client.registration.google.client-id:}') || T(org.springframework.util.StringUtils).hasText('${spring.security.oauth2.client.registration.yandex.client-id:}')")
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    static final String REDIRECT_SESSION_KEY = "oauth_redirect";
    static final String ANDROID_REDIRECT = "notekeeper://oauth";

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
        String provider = "google";
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            provider = oauthToken.getAuthorizedClientRegistrationId();
        }

        String providerId;
        String email;
        String name;
        String picture;
        if ("yandex".equals(provider)) {
            providerId = stringAttr(oAuth2User.getAttribute("id"));
            email = yandexEmail(oAuth2User);
            name = firstNonBlank(
                    oAuth2User.getAttribute("display_name"),
                    oAuth2User.getAttribute("real_name"),
                    oAuth2User.getAttribute("login")
            );
            picture = yandexAvatar(oAuth2User);
        } else {
            providerId = oAuth2User.getAttribute("sub");
            email = oAuth2User.getAttribute("email");
            name = oAuth2User.getAttribute("name");
            picture = oAuth2User.getAttribute("picture");
        }

        log.info("OAuth2 success provider={} email={}", provider, email);

        AuthResponse authResponse = authService.loginWithOAuth(provider, providerId, email, name, picture);
        String token = authResponse.getToken();

        String mobileRedirect = takeMobileRedirect(request);
        if (mobileRedirect != null) {
            String redirectUrl = mobileRedirect + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
            log.info("OAuth2 mobile redirect");
            response.sendRedirect(redirectUrl);
            return;
        }

        String baseUrl = resolveBaseUrl(request);
        String redirectUrl = baseUrl + "/#/login?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        log.info("OAuth2 redirect URL: {}/#/login?token=***", baseUrl);
        response.sendRedirect(redirectUrl);
    }

    static boolean isAllowedMobileRedirect(String redirect) {
        return ANDROID_REDIRECT.equals(redirect);
    }

    private static String takeMobileRedirect(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object stored = session.getAttribute(REDIRECT_SESSION_KEY);
        session.removeAttribute(REDIRECT_SESSION_KEY);
        if (stored instanceof String redirect && isAllowedMobileRedirect(redirect)) {
            return redirect;
        }
        return null;
    }

    static String yandexEmail(OAuth2User user) {
        String email = user.getAttribute("default_email");
        if (email != null && !email.isBlank()) {
            return email;
        }
        Object emails = user.getAttribute("emails");
        if (emails instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (item != null && !item.toString().isBlank()) {
                    return item.toString();
                }
            }
        }
        return null;
    }

    static String yandexAvatar(OAuth2User user) {
        Object empty = user.getAttribute("is_avatar_empty");
        if (Boolean.TRUE.equals(empty) || "true".equalsIgnoreCase(String.valueOf(empty))) {
            return null;
        }
        String avatarId = stringAttr(user.getAttribute("default_avatar_id"));
        if (avatarId == null || avatarId.isBlank() || "0".equals(avatarId)) {
            return null;
        }
        return "https://avatars.yandex.net/get-yapic/" + avatarId + "/islands-200";
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String stringAttr(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Public URL after OAuth login. Synology Reverse Proxy often sends
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

package dev.cogitor.note.keeper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import dev.cogitor.note.keeper.config.PrometheusMetrics;
import dev.cogitor.note.keeper.dto.AuthRequest;
import dev.cogitor.note.keeper.dto.AuthResponse;
import dev.cogitor.note.keeper.mapper.UserCredentialsMapper;
import dev.cogitor.note.keeper.mapper.UserMapper;
import dev.cogitor.note.keeper.model.User;
import dev.cogitor.note.keeper.model.UserCredentials;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Authentication service supporting password and Google OAuth.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final UserCredentialsMapper credentialsMapper;
    private final JwtService jwtService;
    private final PasswordService passwordService;
    private final PrometheusMetrics metrics;

    /**
     * Register user with email and password.
     */
    public AuthResponse registerWithEmail(AuthRequest request) {
        // Check if user exists
        User existingUser = userMapper.findByEmail(request.getEmail());
        if (existingUser != null) {
            throw new RuntimeException("User with email already exists: " + request.getEmail());
        }

        // Create user with generated avatar for local accounts
        String userId = UUID.randomUUID().toString();
        User user = new User();
        user.setId(userId);
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setAvatarUrl(generateAvatarUrl(request.getEmail()));
        user.setProvider("local");
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);

        // Create credentials
        String passwordHash = passwordService.hashPassword(request.getPassword());

        UserCredentials credentials = new UserCredentials();
        credentials.setUserId(userId);
        credentials.setPasswordHash(passwordHash);
        credentials.setSalt("");
        credentials.setCreatedAt(LocalDateTime.now());
        credentials.setUpdatedAt(LocalDateTime.now());

        credentialsMapper.insert(credentials);

        // Generate JWT token
        String token = jwtService.generateToken(userId, user.getEmail());

        return new AuthResponse(token, user);
    }

    /**
     * Login with email and password.
     */
    public AuthResponse loginWithEmail(AuthRequest request) {
        User user = userMapper.findByEmail(request.getEmail());
        if (user == null) {
            metrics.login("email", false);
            throw new RuntimeException("Invalid email or password");
        }

        UserCredentials credentials = credentialsMapper.findByUserId(user.getId());
        if (credentials == null) {
            metrics.login("email", false);
            throw new RuntimeException("Invalid email or password");
        }

        if (!passwordService.matches(request.getPassword(), credentials)) {
            metrics.login("email", false);
            throw new RuntimeException("Invalid email or password");
        }

        upgradeLegacyPasswordIfNeeded(request.getPassword(), credentials);

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        metrics.login("email", true);
        return new AuthResponse(token, user);
    }

    /**
     * Login or register with Google OAuth.
     * Called by OAuth2SuccessHandler after Spring Security verifies the token.
     */
    public AuthResponse loginWithGoogle(String googleId, String email, String name, String avatarUrl) {
        return loginWithOAuth("google", googleId, email, name, avatarUrl);
    }

    public AuthResponse loginWithOAuth(String provider, String providerUserId, String email, String name, String avatarUrl) {
        if (!"google".equals(provider) && !"yandex".equals(provider)) {
            metrics.login("unknown", false);
            throw new RuntimeException("Unsupported OAuth provider: " + provider);
        }
        if (providerUserId == null || providerUserId.isBlank()) {
            metrics.login(provider, false);
            throw new RuntimeException("OAuth provider id is missing");
        }
        if (email == null || email.isBlank()) {
            metrics.login(provider, false);
            throw new RuntimeException("OAuth account has no email");
        }

        User user = "google".equals(provider)
                ? userMapper.findByGoogleId(providerUserId)
                : userMapper.findByYandexId(providerUserId);

        if (user == null) {
            user = userMapper.findByEmail(email);
            if (user != null) {
                applyProviderId(user, provider, providerUserId);
                if ("local".equals(user.getProvider()) || user.getProvider() == null) {
                    user.setProvider(provider);
                }
                if (avatarUrl != null && !avatarUrl.isBlank()) {
                    user.setAvatarUrl(avatarUrl);
                }
                if (name != null && !name.isBlank()) {
                    user.setName(name);
                }
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.update(user);
            } else {
                String userId = UUID.randomUUID().toString();
                user = new User();
                user.setId(userId);
                user.setEmail(email);
                user.setName(name != null && !name.isBlank() ? name : email);
                user.setAvatarUrl(avatarUrl);
                user.setProvider(provider);
                applyProviderId(user, provider, providerUserId);
                user.setActive(true);
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.insert(user);
            }
        } else {
            user.setEmail(email);
            if (name != null && !name.isBlank()) {
                user.setName(name);
            }
            if (avatarUrl != null && !avatarUrl.isBlank()) {
                user.setAvatarUrl(avatarUrl);
            }
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.update(user);
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        metrics.login(provider, true);
        return new AuthResponse(token, user);
    }

    /**
     * Attach Google/Yandex to an existing logged-in user (emails may differ).
     */
    public User linkOAuth(String userId, String provider, String providerUserId, String email) {
        if (!"google".equals(provider) && !"yandex".equals(provider)) {
            throw new RuntimeException("Unsupported OAuth provider: " + provider);
        }
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new RuntimeException("OAuth provider id is missing");
        }
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        User owner = "google".equals(provider)
                ? userMapper.findByGoogleId(providerUserId)
                : userMapper.findByYandexId(providerUserId);
        if (owner != null && !owner.getId().equals(userId)) {
            throw new RuntimeException("This " + provider + " account is already linked to another user");
        }
        applyProviderId(user, provider, providerUserId);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);
        log.info("Linked {} to user {} (oauth email={})", provider, userId, email);
        return user;
    }

    public User unlinkOAuth(String userId, String provider) {
        if (!"google".equals(provider) && !"yandex".equals(provider)) {
            throw new RuntimeException("Unsupported OAuth provider: " + provider);
        }
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        boolean hasPassword = credentialsMapper.findByUserId(userId) != null;
        boolean hasGoogle = user.getGoogleId() != null && !user.getGoogleId().isBlank();
        boolean hasYandex = user.getYandexId() != null && !user.getYandexId().isBlank();
        if ("google".equals(provider)) {
            if (!hasPassword && !hasYandex) {
                throw new RuntimeException("Cannot unlink the last sign-in method");
            }
            user.setGoogleId(null);
            if ("google".equals(user.getProvider())) {
                user.setProvider(hasYandex ? "yandex" : "local");
            }
        } else {
            if (!hasPassword && !hasGoogle) {
                throw new RuntimeException("Cannot unlink the last sign-in method");
            }
            user.setYandexId(null);
            if ("yandex".equals(user.getProvider())) {
                user.setProvider(hasGoogle ? "google" : "local");
            }
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);
        return user;
    }

    private static void applyProviderId(User user, String provider, String providerUserId) {
        if ("google".equals(provider)) {
            user.setGoogleId(providerUserId);
        } else {
            user.setYandexId(providerUserId);
        }
    }

    /**
     * Validate JWT token and return user.
     */
    public User validateToken(String token) {
        String userId = jwtService.validateToken(token);
        if (userId == null) {
            return null;
        }
        return userMapper.findById(userId);
    }

    private void upgradeLegacyPasswordIfNeeded(String plainPassword, UserCredentials credentials) {
        if (!passwordService.isLegacyHash(credentials)) {
            return;
        }
        credentials.setPasswordHash(passwordService.hashPassword(plainPassword));
        credentials.setSalt("");
        credentials.setUpdatedAt(LocalDateTime.now());
        credentialsMapper.update(credentials);
        log.info("Upgraded legacy password hash for user {}", credentials.getUserId());
    }

    private String generateAvatarUrl(String email) {
        String seed = email.toLowerCase().trim();
        return "https://api.dicebear.com/7.x/avataaars/svg?seed="
                + java.net.URLEncoder.encode(seed, StandardCharsets.UTF_8);
    }
}

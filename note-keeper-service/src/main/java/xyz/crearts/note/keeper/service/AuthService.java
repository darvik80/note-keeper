package xyz.crearts.note.keeper.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import xyz.crearts.note.keeper.dto.AuthRequest;
import xyz.crearts.note.keeper.dto.AuthResponse;
import xyz.crearts.note.keeper.mapper.UserCredentialsMapper;
import xyz.crearts.note.keeper.mapper.UserMapper;
import xyz.crearts.note.keeper.model.User;
import xyz.crearts.note.keeper.model.UserCredentials;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Authentication service supporting password and Google OAuth.
 */
@Slf4j
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final UserCredentialsMapper credentialsMapper;
    private final JwtService jwtService;
    private final PasswordService passwordService;

    public AuthService(UserMapper userMapper, UserCredentialsMapper credentialsMapper,
                       JwtService jwtService, PasswordService passwordService) {
        this.userMapper = userMapper;
        this.credentialsMapper = credentialsMapper;
        this.jwtService = jwtService;
        this.passwordService = passwordService;
    }

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
            throw new RuntimeException("Invalid email or password");
        }

        if (!"local".equals(user.getProvider())) {
            throw new RuntimeException("User registered with " + user.getProvider() + ", not password");
        }

        UserCredentials credentials = credentialsMapper.findByUserId(user.getId());
        if (credentials == null) {
            throw new RuntimeException("Invalid email or password");
        }

        if (!passwordService.matches(request.getPassword(), credentials)) {
            throw new RuntimeException("Invalid email or password");
        }

        upgradeLegacyPasswordIfNeeded(request.getPassword(), credentials);

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user);
    }

    /**
     * Login or register with Google OAuth.
     * Called by OAuth2SuccessHandler after Spring Security verifies the token.
     */
    public AuthResponse loginWithGoogle(String googleId, String email, String name, String avatarUrl) {
        User user = userMapper.findByGoogleId(googleId);
        
        if (user == null) {
            // Check if user exists by email
            user = userMapper.findByEmail(email);
            if (user != null) {
                // Link Google account to existing user
                user.setGoogleId(googleId);
                user.setProvider("google");
                user.setAvatarUrl(avatarUrl);
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.update(user);
            } else {
                // Create new user
                String userId = UUID.randomUUID().toString();
                user = new User();
                user.setId(userId);
                user.setEmail(email);
                user.setName(name);
                user.setAvatarUrl(avatarUrl);
                user.setProvider("google");
                user.setGoogleId(googleId);
                user.setActive(true);
                user.setCreatedAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
                userMapper.insert(user);
            }
        } else {
            // Update user info
            user.setEmail(email);
            user.setName(name);
            user.setAvatarUrl(avatarUrl);
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.update(user);
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user);
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

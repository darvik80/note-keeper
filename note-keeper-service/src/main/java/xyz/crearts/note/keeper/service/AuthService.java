package xyz.crearts.note.keeper.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import xyz.crearts.note.keeper.dto.AuthRequest;
import xyz.crearts.note.keeper.dto.AuthResponse;
import xyz.crearts.note.keeper.mapper.UserCredentialsMapper;
import xyz.crearts.note.keeper.mapper.UserMapper;
import xyz.crearts.note.keeper.model.User;
import xyz.crearts.note.keeper.model.UserCredentials;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * Authentication service supporting password and Google OAuth.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserMapper userMapper;
    private final UserCredentialsMapper credentialsMapper;
    private final JwtService jwtService;

    public AuthService(UserMapper userMapper, UserCredentialsMapper credentialsMapper, JwtService jwtService) {
        this.userMapper = userMapper;
        this.credentialsMapper = credentialsMapper;
        this.jwtService = jwtService;
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

        // Create user
        String userId = UUID.randomUUID().toString();
        User user = new User();
        user.setId(userId);
        user.setEmail(request.getEmail());
        user.setName(request.getName());
        user.setProvider("local");
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);

        // Create credentials
        String salt = generateSalt();
        String passwordHash = hashPassword(request.getPassword(), salt);

        UserCredentials credentials = new UserCredentials();
        credentials.setUserId(userId);
        credentials.setPasswordHash(passwordHash);
        credentials.setSalt(salt);
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

        String inputHash = hashPassword(request.getPassword(), credentials.getSalt());
        if (!inputHash.equals(credentials.getPasswordHash())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user);
    }

    /**
     * Login or register with Google OAuth.
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

    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hashedBytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
}

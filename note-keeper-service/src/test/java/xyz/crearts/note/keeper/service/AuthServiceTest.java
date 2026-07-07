package xyz.crearts.note.keeper.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.crearts.note.keeper.dto.AuthRequest;
import xyz.crearts.note.keeper.dto.AuthResponse;
import xyz.crearts.note.keeper.mapper.UserCredentialsMapper;
import xyz.crearts.note.keeper.mapper.UserMapper;
import xyz.crearts.note.keeper.model.User;
import xyz.crearts.note.keeper.model.UserCredentials;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private UserCredentialsMapper credentialsMapper;
    @Mock private JwtService jwtService;

    private final PasswordService passwordService = new PasswordService();
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userMapper, credentialsMapper, jwtService, passwordService);
    }

    @Test
    void registerWithEmail_newUser_shouldCreateUserAndReturnToken() {
        AuthRequest request = new AuthRequest();
        request.setEmail("new@test.com");
        request.setPassword("password123");
        request.setName("New User");

        when(userMapper.findByEmail("new@test.com")).thenReturn(null);
        when(jwtService.generateToken(anyString(), eq("new@test.com"))).thenReturn("jwt-token");

        AuthResponse response = authService.registerWithEmail(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        verify(userMapper).insert(any(User.class));
        verify(credentialsMapper).insert(argThat(credentials ->
                passwordService.isBcryptHash(credentials.getPasswordHash())));
    }

    @Test
    void registerWithEmail_existingUser_shouldThrowException() {
        AuthRequest request = new AuthRequest();
        request.setEmail("existing@test.com");
        request.setPassword("password123");

        User existingUser = new User();
        existingUser.setEmail("existing@test.com");
        when(userMapper.findByEmail("existing@test.com")).thenReturn(existingUser);

        assertThrows(RuntimeException.class, () -> authService.registerWithEmail(request));
        verify(userMapper, never()).insert(any());
    }

    @Test
    void loginWithEmail_validCredentials_shouldReturnToken() {
        AuthRequest request = new AuthRequest();
        request.setEmail("user@test.com");
        request.setPassword("correct-password");

        User user = new User();
        user.setId("user-id");
        user.setEmail("user@test.com");
        user.setProvider("local");
        when(userMapper.findByEmail("user@test.com")).thenReturn(user);

        UserCredentials credentials = new UserCredentials();
        credentials.setUserId("user-id");
        credentials.setSalt("");
        credentials.setPasswordHash(passwordService.hashPassword("correct-password"));
        when(credentialsMapper.findByUserId("user-id")).thenReturn(credentials);
        when(jwtService.generateToken("user-id", "user@test.com")).thenReturn("jwt-token");

        AuthResponse response = authService.loginWithEmail(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
    }

    @Test
    void loginWithEmail_legacySha256_shouldUpgradeHash() {
        AuthRequest request = new AuthRequest();
        request.setEmail("legacy@test.com");
        request.setPassword("legacy-password");

        User user = new User();
        user.setId("legacy-user");
        user.setEmail("legacy@test.com");
        user.setProvider("local");
        when(userMapper.findByEmail("legacy@test.com")).thenReturn(user);

        UserCredentials credentials = new UserCredentials();
        credentials.setUserId("legacy-user");
        credentials.setSalt("legacy-salt");
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            md.update("legacy-salt".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            credentials.setPasswordHash(java.util.Base64.getEncoder().encodeToString(
                    md.digest("legacy-password".getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        } catch (Exception e) {
            fail(e);
        }

        when(credentialsMapper.findByUserId("legacy-user")).thenReturn(credentials);
        when(jwtService.generateToken("legacy-user", "legacy@test.com")).thenReturn("jwt-token");

        authService.loginWithEmail(request);

        verify(credentialsMapper).update(argThat(updated ->
                passwordService.isBcryptHash(updated.getPasswordHash()) && "".equals(updated.getSalt())));
    }

    @Test
    void loginWithEmail_unknownEmail_shouldThrowException() {
        AuthRequest request = new AuthRequest();
        request.setEmail("unknown@test.com");
        request.setPassword("password");

        when(userMapper.findByEmail("unknown@test.com")).thenReturn(null);

        assertThrows(RuntimeException.class, () -> authService.loginWithEmail(request));
    }

    @Test
    void loginWithEmail_googleProvider_shouldThrowException() {
        AuthRequest request = new AuthRequest();
        request.setEmail("google@test.com");
        request.setPassword("password");

        User user = new User();
        user.setId("user-id");
        user.setProvider("google");
        when(userMapper.findByEmail("google@test.com")).thenReturn(user);

        assertThrows(RuntimeException.class, () -> authService.loginWithEmail(request));
    }

    @Test
    void loginWithGoogle_newUser_shouldCreateUser() {
        when(userMapper.findByGoogleId("google-id")).thenReturn(null);
        when(userMapper.findByEmail("google@test.com")).thenReturn(null);
        when(jwtService.generateToken(anyString(), eq("google@test.com"))).thenReturn("jwt-token");

        AuthResponse response = authService.loginWithGoogle("google-id", "google@test.com", "Google User", "avatar.jpg");

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void loginWithGoogle_existingGoogleUser_shouldUpdateUser() {
        User existingUser = new User();
        existingUser.setId("user-id");
        existingUser.setGoogleId("google-id");
        when(userMapper.findByGoogleId("google-id")).thenReturn(existingUser);
        when(jwtService.generateToken("user-id", "google@test.com")).thenReturn("jwt-token");

        AuthResponse response = authService.loginWithGoogle("google-id", "google@test.com", "Updated Name", "new-avatar.jpg");

        assertNotNull(response);
        verify(userMapper).update(existingUser);
    }

    @Test
    void loginWithGoogle_existingEmailUser_shouldLinkGoogleAccount() {
        User existingUser = new User();
        existingUser.setId("user-id");
        existingUser.setEmail("user@test.com");
        when(userMapper.findByGoogleId("google-id")).thenReturn(null);
        when(userMapper.findByEmail("user@test.com")).thenReturn(existingUser);
        when(jwtService.generateToken("user-id", "user@test.com")).thenReturn("jwt-token");

        AuthResponse response = authService.loginWithGoogle("google-id", "user@test.com", "User", "avatar.jpg");

        assertNotNull(response);
        verify(userMapper).update(existingUser);
        assertEquals("google-id", existingUser.getGoogleId());
    }

    @Test
    void validateToken_validToken_shouldReturnUser() {
        when(jwtService.validateToken("valid-token")).thenReturn("user-id");
        User user = new User();
        user.setId("user-id");
        when(userMapper.findById("user-id")).thenReturn(user);

        User result = authService.validateToken("valid-token");

        assertNotNull(result);
        assertEquals("user-id", result.getId());
    }

    @Test
    void validateToken_invalidToken_shouldReturnNull() {
        when(jwtService.validateToken("invalid-token")).thenReturn(null);

        User result = authService.validateToken("invalid-token");

        assertNull(result);
        verify(userMapper, never()).findById(any());
    }
}

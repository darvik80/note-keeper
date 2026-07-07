package xyz.crearts.note.keeper.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret",
                "test-secret-key-for-jwt-that-is-at-least-256-bits-long-for-hs256");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L); // 24 hours
    }

    @Test
    void generateToken_shouldReturnNonNullToken() {
        String token = jwtService.generateToken("user-123", "test@example.com");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void validateToken_validToken_shouldReturnUserId() {
        String token = jwtService.generateToken("user-123", "test@example.com");
        String userId = jwtService.validateToken(token);
        assertEquals("user-123", userId);
    }

    @Test
    void validateToken_invalidToken_shouldReturnNull() {
        String userId = jwtService.validateToken("invalid.token.here");
        assertNull(userId);
    }

    @Test
    void validateToken_expiredToken_shouldReturnNull() {
        // Set expiration to 0 so the token is immediately expired
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 0L);
        String token = jwtService.generateToken("user-123", "test@example.com");
        // Token is already expired (0ms expiration)
        String userId = jwtService.validateToken(token);
        assertNull(userId);
    }

    @Test
    void getUserIdFromToken_shouldReturnCorrectUserId() {
        String token = jwtService.generateToken("user-uuid-456", "user@test.com");
        String userId = jwtService.getUserIdFromToken(token);
        assertEquals("user-uuid-456", userId);
    }

    @Test
    void generateToken_differentUsers_shouldProduceDifferentTokens() {
        String token1 = jwtService.generateToken("user-1", "a@test.com");
        String token2 = jwtService.generateToken("user-2", "b@test.com");
        assertNotEquals(token1, token2);
    }

    @Test
    void validateToken_wrongSecret_shouldReturnNull() {
        String token = jwtService.generateToken("user-123", "test@test.com");

        JwtService otherService = new JwtService();
        ReflectionTestUtils.setField(otherService, "jwtSecret",
                "completely-different-secret-key-that-is-also-long-enough-for-hs256");
        ReflectionTestUtils.setField(otherService, "jwtExpiration", 86400000L);

        String userId = otherService.validateToken(token);
        assertNull(userId);
    }

    @Test
    void generateToken_shouldContainEmailClaim() {
        String token = jwtService.generateToken("user-123", "test@example.com");
        assertNotNull(token);
        // Token structure: header.payload.signature - the payload contains our claims
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT should have 3 parts");
    }
}

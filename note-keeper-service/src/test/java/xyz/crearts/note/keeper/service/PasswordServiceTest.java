package xyz.crearts.note.keeper.service;

import org.junit.jupiter.api.Test;
import xyz.crearts.note.keeper.model.UserCredentials;

import static org.junit.jupiter.api.Assertions.*;

class PasswordServiceTest {

    private final PasswordService passwordService = new PasswordService();

    @Test
    void hashPassword_shouldProduceBcryptHash() {
        String hash = passwordService.hashPassword("secret");
        assertTrue(passwordService.isBcryptHash(hash));
        assertTrue(passwordService.matches("secret", credentialsWithHash(hash)));
    }

    @Test
    void matches_wrongPassword_shouldReturnFalse() {
        String hash = passwordService.hashPassword("secret");
        assertFalse(passwordService.matches("wrong", credentialsWithHash(hash)));
    }

    @Test
    void matches_legacySha256_shouldVerifyAndDetectLegacy() {
        UserCredentials credentials = new UserCredentials();
        credentials.setSalt("legacy-salt");
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            md.update("legacy-salt".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            credentials.setPasswordHash(java.util.Base64.getEncoder().encodeToString(
                    md.digest("legacy-password".getBytes(java.nio.charset.StandardCharsets.UTF_8))));
        } catch (Exception e) {
            fail(e);
        }

        assertTrue(passwordService.isLegacyHash(credentials));
        assertTrue(passwordService.matches("legacy-password", credentials));
        assertFalse(passwordService.matches("wrong", credentials));
    }

    private UserCredentials credentialsWithHash(String hash) {
        UserCredentials credentials = new UserCredentials();
        credentials.setPasswordHash(hash);
        credentials.setSalt("");
        return credentials;
    }
}

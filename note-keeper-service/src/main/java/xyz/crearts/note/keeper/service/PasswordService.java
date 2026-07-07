package xyz.crearts.note.keeper.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import xyz.crearts.note.keeper.model.UserCredentials;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Service
public class PasswordService {

    private static final int BCRYPT_STRENGTH = 12;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(BCRYPT_STRENGTH);

    public String hashPassword(String plainPassword) {
        return encoder.encode(plainPassword);
    }

    public boolean matches(String plainPassword, UserCredentials credentials) {
        String storedHash = credentials.getPasswordHash();
        if (isBcryptHash(storedHash)) {
            return encoder.matches(plainPassword, storedHash);
        }
        return matchesLegacySha256(plainPassword, credentials);
    }

    public boolean isBcryptHash(String hash) {
        return hash != null && hash.startsWith("$2");
    }

    public boolean isLegacyHash(UserCredentials credentials) {
        return credentials != null && !isBcryptHash(credentials.getPasswordHash());
    }

    private boolean matchesLegacySha256(String plainPassword, UserCredentials credentials) {
        if (credentials.getSalt() == null || credentials.getSalt().isBlank()) {
            return false;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(credentials.getSalt().getBytes(StandardCharsets.UTF_8));
            byte[] hashedBytes = md.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            String inputHash = Base64.getEncoder().encodeToString(hashedBytes);
            return inputHash.equals(credentials.getPasswordHash());
        } catch (Exception e) {
            throw new RuntimeException("Error verifying legacy password hash", e);
        }
    }
}

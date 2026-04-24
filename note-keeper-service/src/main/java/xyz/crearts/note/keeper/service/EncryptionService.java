package xyz.crearts.note.keeper.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption service for note content.
 */
@Slf4j
@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int KEY_SIZE = 256;

    private final SecretKeySpec secretKey;

    public EncryptionService(@Value("${app.encryption.key:}") String base64Key) {
        if (base64Key == null || base64Key.isEmpty()) {
            // Generate random key for development (NOT for production!)
            log.warn("No encryption key configured. Generating random key (data will be lost on restart).");
            byte[] randomKey = new byte[KEY_SIZE / 8];
            new SecureRandom().nextBytes(randomKey);
            this.secretKey = new SecretKeySpec(randomKey, ALGORITHM);
        } else {
            byte[] decodedKey = Base64.getDecoder().decode(base64Key);
            if (decodedKey.length != KEY_SIZE / 8) {
                throw new IllegalArgumentException("Encryption key must be " + KEY_SIZE + " bits (Base64 encoded)");
            }
            this.secretKey = new SecretKeySpec(decodedKey, ALGORITHM);
            log.info("Encryption key loaded successfully");
        }
    }

    /**
     * Encrypts plaintext content.
     * Format: IV (12 bytes) + Ciphertext + Auth Tag
     * Returns Base64 encoded string.
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV + ciphertext
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Failed to encrypt content", e);
        }
    }

    /**
     * Decrypts Base64 encoded ciphertext.
     * Expects format: IV (12 bytes) + Ciphertext + Auth Tag
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            byte[] combined = Base64.getDecoder().decode(encryptedText);

            if (combined.length < GCM_IV_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted data length");
            }

            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);

            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Failed to decrypt content", e);
        }
    }

    /**
     * Generates a new random encryption key (Base64 encoded).
     * Use this to generate a production-ready key.
     */
    public static String generateKey() {
        byte[] key = new byte[KEY_SIZE / 8];
        new SecureRandom().nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
}

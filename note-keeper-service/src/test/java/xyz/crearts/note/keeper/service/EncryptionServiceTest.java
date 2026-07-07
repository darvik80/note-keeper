package xyz.crearts.note.keeper.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        // Generate a valid 32-byte Base64 key
        String base64Key = Base64.getEncoder().encodeToString(new byte[32]);
        encryptionService = new EncryptionService(base64Key);
    }

    @Test
    void encryptAndDecrypt_shouldReturnOriginalText() {
        String plaintext = "Hello, World!";
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void encrypt_shouldReturnDifferentCiphertextsForSameInput() {
        String plaintext = "same text";
        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);
        // Different IVs should produce different ciphertexts
        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    void encrypt_nullInput_shouldReturnNull() {
        assertNull(encryptionService.encrypt(null));
    }

    @Test
    void encrypt_emptyInput_shouldReturnEmpty() {
        assertEquals("", encryptionService.encrypt(""));
    }

    @Test
    void decrypt_nullInput_shouldReturnNull() {
        assertNull(encryptionService.decrypt(null));
    }

    @Test
    void decrypt_emptyInput_shouldReturnEmpty() {
        assertEquals("", encryptionService.decrypt(""));
    }

    @Test
    void decrypt_invalidData_shouldThrowException() {
        String invalidBase64 = Base64.getEncoder().encodeToString(new byte[5]); // too short for IV
        assertThrows(RuntimeException.class, () -> encryptionService.decrypt(invalidBase64));
    }

    @Test
    void decrypt_tamperedData_shouldThrowException() {
        String encrypted = encryptionService.encrypt("secret data");
        // Tamper with the encrypted data
        byte[] decoded = Base64.getDecoder().decode(encrypted);
        decoded[decoded.length - 1] ^= 0xFF;
        String tampered = Base64.getEncoder().encodeToString(decoded);
        assertThrows(RuntimeException.class, () -> encryptionService.decrypt(tampered));
    }

    @Test
    void encrypt_longText_shouldWorkCorrectly() {
        String longText = "a".repeat(10_000);
        String encrypted = encryptionService.encrypt(longText);
        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(longText, decrypted);
    }

    @Test
    void encrypt_unicodeText_shouldWorkCorrectly() {
        String unicode = "Привет мир! 你好世界 🌍";
        String encrypted = encryptionService.encrypt(unicode);
        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(unicode, decrypted);
    }

    @Test
    void constructor_emptyKey_shouldGenerateRandomKey() {
        EncryptionService service = new EncryptionService("");
        String encrypted = service.encrypt("test");
        String decrypted = service.decrypt(encrypted);
        assertEquals("test", decrypted);
    }

    @Test
    void constructor_invalidKeyLength_shouldThrowException() {
        String shortKey = Base64.getEncoder().encodeToString(new byte[16]); // 16 bytes instead of 32
        assertThrows(IllegalArgumentException.class, () -> new EncryptionService(shortKey));
    }

    @Test
    void generateKey_shouldReturnValidBase64Key() {
        String key = EncryptionService.generateKey();
        assertNotNull(key);
        byte[] decoded = Base64.getDecoder().decode(key);
        assertEquals(32, decoded.length);
    }

    @Test
    void differentKeys_shouldNotDecryptEachOther() {
        String key1 = Base64.getEncoder().encodeToString(new byte[32]);
        String key2 = Base64.getEncoder().encodeToString(new byte[32]);
        key2 = Base64.getEncoder().encodeToString(new byte[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32});

        EncryptionService service1 = new EncryptionService(key1);
        EncryptionService service2 = new EncryptionService(key2);

        String encrypted = service1.encrypt("secret");
        assertThrows(RuntimeException.class, () -> service2.decrypt(encrypted));
    }
}

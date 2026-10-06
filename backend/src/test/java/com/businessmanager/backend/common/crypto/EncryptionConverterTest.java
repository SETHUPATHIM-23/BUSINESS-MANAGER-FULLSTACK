package com.businessmanager.backend.common.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncryptionConverterTest {

    private static final String SECRET = "EnterpriseKey123";

    private EncryptionConverter converter;

    @BeforeEach
    void setUp() {
        converter = new EncryptionConverter();
        converter.setSecret(SECRET);
    }

    @Test
    void convertToDatabaseColumn_UsesVersionedAuthenticatedEncryption() {
        String encrypted = converter.convertToDatabaseColumn("Account 12345");

        assertNotNull(encrypted);
        assertTrue(encrypted.startsWith("v2:"));
        assertEquals("Account 12345", converter.convertToEntityAttribute(encrypted));
    }

    @Test
    void convertToDatabaseColumn_UsesRandomIvForSamePlaintext() {
        String first = converter.convertToDatabaseColumn("Sensitive salary data");
        String second = converter.convertToDatabaseColumn("Sensitive salary data");

        assertNotEquals(first, second);
        assertEquals("Sensitive salary data", converter.convertToEntityAttribute(first));
        assertEquals("Sensitive salary data", converter.convertToEntityAttribute(second));
    }

    @Test
    void convertToEntityAttribute_ReadsLegacyEcbValues() throws Exception {
        String legacyEncrypted = encryptLegacyValue("Legacy bank details");

        assertEquals("Legacy bank details", converter.convertToEntityAttribute(legacyEncrypted));
    }

    private String encryptLegacyValue(String value) throws Exception {
        byte[] keyBytes = new byte[16];
        byte[] secretBytes = SECRET.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(secretBytes, 0, keyBytes, 0, Math.min(secretBytes.length, 16));

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, "AES"));
        return Base64.getEncoder().encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}

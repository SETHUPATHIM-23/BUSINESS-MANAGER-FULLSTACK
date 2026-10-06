package com.businessmanager.backend.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Converter
@Component
public class EncryptionConverter implements AttributeConverter<String, String> {

    private static final String CURRENT_PREFIX = "v2:";
    private static final String GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final String LEGACY_ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static SecretKeySpec key;

    @Value("${app.crypto.secret:MySuperSecretKey1}") // Configurable, defaults to 16 bytes for AES-128
    public void setSecret(String secret) {
        initKey(secret);
    }

    private synchronized void initKey(String secret) {
        byte[] bytes = new byte[16];
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(secretBytes, 0, bytes, 0, Math.min(secretBytes.length, 16));
        key = new SecretKeySpec(bytes, "AES");
    }

    private SecretKeySpec getKey() {
        if (key == null) {
            initKey("MySuperSecretKey1");
        }
        return key;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.trim().isEmpty()) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            SECURE_RANDOM.nextBytes(iv);

            Cipher c = Cipher.getInstance(GCM_ALGORITHM);
            c.init(Cipher.ENCRYPT_MODE, getKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] encrypted = c.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);

            return CURRENT_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            if (!dbData.startsWith(CURRENT_PREFIX)) {
                return decryptLegacyValue(dbData);
            }

            byte[] payload = Base64.getDecoder().decode(dbData.substring(CURRENT_PREFIX.length()));
            if (payload.length <= GCM_IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Encrypted value is too short");
            }

            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            byte[] encrypted = new byte[payload.length - GCM_IV_LENGTH_BYTES];
            System.arraycopy(payload, 0, iv, 0, iv.length);
            System.arraycopy(payload, iv.length, encrypted, 0, encrypted.length);

            Cipher c = Cipher.getInstance(GCM_ALGORITHM);
            c.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(c.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    private String decryptLegacyValue(String dbData) throws Exception {
        Cipher c = Cipher.getInstance(LEGACY_ALGORITHM);
        c.init(Cipher.DECRYPT_MODE, getKey());
        return new String(c.doFinal(Base64.getDecoder().decode(dbData)), StandardCharsets.UTF_8);
    }
}

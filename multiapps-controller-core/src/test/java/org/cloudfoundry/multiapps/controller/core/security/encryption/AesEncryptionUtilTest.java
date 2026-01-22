package org.cloudfoundry.multiapps.controller.core.security.encryption;

import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Arrays;
import javax.crypto.Cipher;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AesEncryptionUtilTest {

    private static final byte[] KEY_FOR_256_32_BYTES = "abcdefghijklmnopqrstuvwxyz123456".getBytes(StandardCharsets.UTF_8);

    @BeforeAll
    static void addBouncyCastleProvider() throws Exception {
        if (Security.getProvider(BouncyCastleFipsProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleFipsProvider());
        }

        Cipher.getInstance("AES/GCM/NoPadding", BouncyCastleFipsProvider.PROVIDER_NAME);
    }

    @Test
    void testEncryptDecryptFlowWhenShortString() {
        String plainText = "hello";
        byte[] encrypted = AesEncryptionUtil.encrypt(plainText, KEY_FOR_256_32_BYTES);
        assertNotNull(encrypted);
        assertTrue(encrypted.length > 12, "encrypted text must include 12-byte IV (initialization vector, nonce) + GCM tag + cipher");

        String decrypted = AesEncryptionUtil.decrypt(encrypted, KEY_FOR_256_32_BYTES);
        assertEquals(plainText, decrypted);
    }

    @Test
    void testEncryptDecryptFlowWhenEmptyString() {
        String emptyString = "";
        byte[] encrypted = AesEncryptionUtil.encrypt(emptyString, KEY_FOR_256_32_BYTES);
        assertNotNull(encrypted);
        String decrypted = AesEncryptionUtil.decrypt(encrypted, KEY_FOR_256_32_BYTES);
        assertEquals(emptyString, decrypted);
    }

    @Test
    void testEncryptionThatInitializationVectorsDifferent() {
        String plainText = "same text";
        byte[] firstEncrypted = AesEncryptionUtil.encrypt(plainText, KEY_FOR_256_32_BYTES);
        byte[] secondEncrypted = AesEncryptionUtil.encrypt(plainText, KEY_FOR_256_32_BYTES);

        assertNotNull(firstEncrypted);
        assertNotNull(secondEncrypted);
        assertNotEquals(Arrays.toString(firstEncrypted), Arrays.toString(secondEncrypted));

        byte[] firstInitializationVector = Arrays.copyOfRange(firstEncrypted, 0, 12);
        byte[] secondInitializationVector = Arrays.copyOfRange(secondEncrypted, 0, 12);
        assertFalse(Arrays.equals(firstInitializationVector, secondInitializationVector));
    }

    @Test
    void testDecryptWhenEncryptedValueCorrupted() {
        String plainText = "real message";
        byte[] encrypted = AesEncryptionUtil.encrypt(plainText, KEY_FOR_256_32_BYTES);

        byte[] tampered = Arrays.copyOf(encrypted, encrypted.length);
        tampered[tampered.length - 1] = (byte) (tampered[tampered.length - 1] ^ 0x01);

        assertThrows(Exception.class, () -> AesEncryptionUtil.decrypt(tampered, KEY_FOR_256_32_BYTES));
    }

    @Test
    void testEncryptWhenWrongEncryptionKetLength() {
        byte[] wrongEncryptionKey = new byte[31];
        assertThrows(Exception.class, () -> AesEncryptionUtil.encrypt("simple text", wrongEncryptionKey));
    }

    @Test
    void testEncryptDecryptFlowWhenWrongEncryptionKey() {
        String plainText = "top secret";
        byte[] encrypted = AesEncryptionUtil.encrypt(plainText, KEY_FOR_256_32_BYTES);

        byte[] differentValidKey = "0123456789abcdef0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);
        assertThrows(Exception.class, () -> AesEncryptionUtil.decrypt(encrypted, differentValidKey));
    }

    @Test
    void testDecryptWhenEncryptedValueTooShort() {
        byte[] tooShort = new byte[7];
        assertThrows(Exception.class, () -> AesEncryptionUtil.decrypt(tooShort, KEY_FOR_256_32_BYTES));
    }

}


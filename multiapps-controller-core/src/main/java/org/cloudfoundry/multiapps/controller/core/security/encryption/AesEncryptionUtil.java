package org.cloudfoundry.multiapps.controller.core.security.encryption;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.core.Messages;

public class AesEncryptionUtil {

    public static byte[] encrypt(String plainText, byte[] encryptionKey) {
        try {
            byte[] gcmInitialisationVector = new byte[Constants.INITIALISATION_VECTOR_LENGTH];
            new SecureRandom().nextBytes(gcmInitialisationVector);

            Cipher cipherObject = Cipher.getInstance(Constants.CYPHER_TRANSFORMATION_NAME, BouncyCastleFipsProvider.PROVIDER_NAME);
            SecretKeySpec secretKeySpec = new SecretKeySpec(encryptionKey, Constants.ENCRYPTION_DECRYPTION_ALGORITHM_NAME);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(Constants.GCM_AUTHENTICATION_TAG_LENGTH, gcmInitialisationVector);

            cipherObject.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec);

            byte[] cipherValue = cipherObject.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combinedCypherValueAndInitialisationVector = new byte[gcmInitialisationVector.length + cipherValue.length];

            System.arraycopy(gcmInitialisationVector, 0, combinedCypherValueAndInitialisationVector, 0, gcmInitialisationVector.length);
            System.arraycopy(cipherValue, 0, combinedCypherValueAndInitialisationVector, gcmInitialisationVector.length,
                             cipherValue.length);

            return combinedCypherValueAndInitialisationVector;
        } catch (Exception e) {
            throw new AESEncryptionException(Messages.ENCRYPTION_BOUNCY_CASTLE_AES256_HAS_FAILED
                                                 + e.getMessage(), e);
        }
    }

    public static String decrypt(byte[] encryptedValue, byte[] encryptionKey) {
        try {
            byte[] gcmInitialisationVector = new byte[Constants.INITIALISATION_VECTOR_LENGTH];
            System.arraycopy(encryptedValue, 0, gcmInitialisationVector, 0,
                             gcmInitialisationVector.length);

            byte[] cipherValue = new byte[encryptedValue.length - 12];
            System.arraycopy(encryptedValue, 12, cipherValue, 0, cipherValue.length);

            Cipher cipherObject = Cipher.getInstance(Constants.CYPHER_TRANSFORMATION_NAME, BouncyCastleFipsProvider.PROVIDER_NAME);
            SecretKeySpec secretKeySpec = new SecretKeySpec(encryptionKey, Constants.ENCRYPTION_DECRYPTION_ALGORITHM_NAME);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(Constants.GCM_AUTHENTICATION_TAG_LENGTH, gcmInitialisationVector);
            cipherObject.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);

            byte[] resultInBytes = cipherObject.doFinal(cipherValue);
            return new String(resultInBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new AESDecryptionException(Messages.DECRYPTION_BOUNCY_CASTLE_AES256_HAS_FAILED
                                                 + e.getMessage(), e);
        }
    }

}

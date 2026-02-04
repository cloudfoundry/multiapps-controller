package org.cloudfoundry.multiapps.controller.core.security.encryption;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.text.MessageFormat;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.core.Messages;

public class AesEncryptionUtil {

    public static byte[] encrypt(String plainText, byte[] encryptionKey) {
        try {
            byte[] gcmInitialisationVector = new byte[Constants.INITIALISATION_VECTOR_LENGTH];
            new SecureRandom().nextBytes(gcmInitialisationVector);

            Cipher cipherObject = setUpCipherObject(encryptionKey, gcmInitialisationVector, Cipher.ENCRYPT_MODE);

            byte[] cipherValue = cipherObject.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combinedCipherValueAndInitialisationVector = new byte[gcmInitialisationVector.length + cipherValue.length];

            System.arraycopy(gcmInitialisationVector, 0, combinedCipherValueAndInitialisationVector, 0, gcmInitialisationVector.length);
            System.arraycopy(cipherValue, 0, combinedCipherValueAndInitialisationVector, gcmInitialisationVector.length,
                             cipherValue.length);

            return combinedCipherValueAndInitialisationVector;
        } catch (Exception e) {
            throw new SLException(MessageFormat.format(Messages.ENCRYPTION_HAS_FAILED, e.getMessage()), e);
        }
    }

    public static String decrypt(byte[] encryptedValue, byte[] encryptionKey) {
        try {
            byte[] gcmInitialisationVector = new byte[Constants.INITIALISATION_VECTOR_LENGTH];
            System.arraycopy(encryptedValue, 0, gcmInitialisationVector, 0,
                             gcmInitialisationVector.length);

            byte[] cipherValue = new byte[encryptedValue.length - Constants.INITIALISATION_VECTOR_LENGTH];
            System.arraycopy(encryptedValue, Constants.INITIALIZATION_VECTOR_POSITION, cipherValue, 0, cipherValue.length);

            Cipher cipherObject = setUpCipherObject(encryptionKey, gcmInitialisationVector, Cipher.DECRYPT_MODE);

            byte[] resultInBytes = cipherObject.doFinal(cipherValue);
            return new String(resultInBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SLException(MessageFormat.format(Messages.DECRYPTION_HAS_FAILED, e.getMessage()), e);
        }
    }

    private static Cipher setUpCipherObject(byte[] encryptionKey, byte[] gcmInitialisationVector, int cipherMode)
        throws InvalidAlgorithmParameterException, InvalidKeyException, NoSuchPaddingException, NoSuchAlgorithmException,
        NoSuchProviderException {
        Cipher cipherObject = Cipher.getInstance(Constants.CIPHER_TRANSFORMATION_NAME, BouncyCastleFipsProvider.PROVIDER_NAME);
        SecretKeySpec secretKeySpec = new SecretKeySpec(encryptionKey, Constants.ENCRYPTION_DECRYPTION_ALGORITHM_NAME);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(Constants.GCM_AUTHENTICATION_TAG_LENGTH, gcmInitialisationVector);

        cipherObject.init(cipherMode, secretKeySpec, gcmParameterSpec);

        return cipherObject;
    }

}

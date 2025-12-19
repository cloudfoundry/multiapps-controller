package org.cloudfoundry.multiapps.controller.process.security.store;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDateTime;

import org.cloudfoundry.multiapps.controller.core.security.encryption.AesEncryptionUtil;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.services.SecretTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecretTokenStoreImpl implements SecretTokenStore {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private SecretTokenService secretTokenService;

    private String encryptionKey;

    private String keyId;

    public SecretTokenStoreImpl(SecretTokenService secretTokenService, String encryptionKey, String keyId) {
        this.secretTokenService = secretTokenService;
        this.encryptionKey = encryptionKey;
        this.keyId = keyId;
    }

    private byte[] keyBytes() {
        return encryptionKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public long put(String processInstanceId, String variableName, String plainText) {
        try {
            byte[] encryptedValue;
            byte[] keyBytes = keyBytes();
            if (plainText == null) {
                encryptedValue = AesEncryptionUtil.encrypt("", keyBytes);
            } else {
                encryptedValue = AesEncryptionUtil.encrypt(plainText, keyBytes);
            }

            return secretTokenService.putSecretToken(processInstanceId, variableName, encryptedValue, keyId);
        } catch (SQLException e) {
            logger.debug(MessageFormat.format(
                Messages.ERROR_INSERTING_SECRET_TOKEN_WITH_VARIABLE_NAME_0_FOR_PROCESS_WITH_ID_1_AND_ENCRYPTION_KEY_ID_2,
                variableName, processInstanceId, keyId));
            throw new SecretTokenStoringException(
                MessageFormat.format(
                    Messages.ERROR_INSERTING_SECRET_TOKEN_WITH_VARIABLE_NAME_0_FOR_PROCESS_WITH_ID_1_AND_ENCRYPTION_KEY_ID_2, variableName,
                    processInstanceId, keyId) + e.getMessage(), e);
        }
    }

    @Override
    public String get(String processInstanceId, long id) {
        try {
            byte[] encryptedValueFromDatabase = secretTokenService.getSecretToken(processInstanceId, id);
            if (encryptedValueFromDatabase == null) {
                return null;
            }
            byte[] keyBytes = keyBytes();
            return AesEncryptionUtil.decrypt(encryptedValueFromDatabase, keyBytes);
        } catch (SQLException e) {
            logger.debug(MessageFormat.format(
                Messages.ERROR_RETRIEVING_SECRET_TOKEN_WITH_ID_0_FOR_PROCESS_WITH_ID_1, id, processInstanceId));
            throw new SecretTokenRetrievalException(
                MessageFormat.format(Messages.ERROR_RETRIEVING_SECRET_TOKEN_WITH_ID_0_FOR_PROCESS_WITH_ID_1, id, processInstanceId)
                    + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String processInstanceId) {
        try {
            secretTokenService.deleteForProcess(processInstanceId);
        } catch (SQLException e) {
            logger.error(MessageFormat.format(Messages.ERROR_DELETING_SECRET_TOKENS_FOR_PROCESS_WITH_ID_0, processInstanceId));
        }
    }

    @Override
    public int deleteOlderThan(LocalDateTime expirationTime) {
        try {
            return secretTokenService.deleteOlderThan(expirationTime);
        } catch (SQLException e) {
            logger.error(Messages.ERROR_DELETING_SECRET_TOKENS_WITH_EXPIRATION_DATE_0, expirationTime);
            return 0;
        }
    }

}

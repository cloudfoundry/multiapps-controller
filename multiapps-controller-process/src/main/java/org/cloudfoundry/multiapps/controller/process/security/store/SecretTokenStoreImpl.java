package org.cloudfoundry.multiapps.controller.process.security.store;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDateTime;

import org.cloudfoundry.multiapps.controller.core.security.encryption.AesEncryptionUtil;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableSecretToken;
import org.cloudfoundry.multiapps.controller.persistence.services.SecretTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecretTokenStoreImpl implements SecretTokenStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretTokenStoreImpl.class);

    private final SecretTokenService secretTokenService;

    private final String encryptionKey;

    public SecretTokenStoreImpl(SecretTokenService secretTokenService, String encryptionKey) {
        this.secretTokenService = secretTokenService;
        this.encryptionKey = encryptionKey;
    }

    @Override
    public long put(String processInstanceId, String variableName, String plainText) {
        byte[] encryptedValue;
        byte[] keyBytes = keyBytes();
        encryptedValue = AesEncryptionUtil.encrypt(plainText, keyBytes);

        long result = secretTokenService.add(ImmutableSecretToken.builder()
                                                                 .processInstanceId(processInstanceId)
                                                                 .variableName(variableName)
                                                                 .content(encryptedValue)
                                                                 .timestamp(LocalDateTime.now())
                                                                 .build())
                                        .getId();
        LOGGER.debug(MessageFormat.format(
            Messages.STORED_SECRET_TOKEN_WITH_VARIABLE_NAME_0_FOR_PROCESS_WITH_ID_1,
            variableName, processInstanceId));
        return result;
    }

    private byte[] keyBytes() {
        return encryptionKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String get(String processInstanceId, long id) {
        byte[] encryptedValueFromDatabase = secretTokenService.createQuery()
                                                              .id(id)
                                                              .singleResult()
                                                              .getContent();
        if (encryptedValueFromDatabase == null) {
            return null;
        }
        byte[] keyBytes = keyBytes();
        String result = AesEncryptionUtil.decrypt(encryptedValueFromDatabase, keyBytes);
        LOGGER.debug(MessageFormat.format(Messages.RETRIEVED_SECRET_TOKEN_WITH_ID_0_FOR_PROCESS_WITH_ID_1, id, processInstanceId));
        return result;
    }

    @Override
    public void deleteByProcessInstanceId(String processInstanceId) {
        secretTokenService.createQuery()
                          .processInstanceId(processInstanceId)
                          .delete();
        LOGGER.debug(MessageFormat.format(Messages.DELETED_SECRET_TOKENS_FOR_PROCESS_WITH_ID_0, processInstanceId));
    }

    @Override
    public int deleteOlderThan(LocalDateTime expirationTime) {
        int result = secretTokenService.createQuery()
                                       .olderThan(expirationTime)
                                       .delete();
        LOGGER.debug(MessageFormat.format(Messages.DELETED_SECRET_TOKENS_WITH_EXPIRATION_DATE_0, expirationTime));
        return result;
    }

}

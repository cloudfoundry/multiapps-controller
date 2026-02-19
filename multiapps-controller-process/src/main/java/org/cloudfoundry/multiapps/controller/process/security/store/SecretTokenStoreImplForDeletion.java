package org.cloudfoundry.multiapps.controller.process.security.store;

import java.text.MessageFormat;
import java.time.LocalDateTime;

import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.services.SecretTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecretTokenStoreImplForDeletion implements SecretTokenStoreDeletion {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretTokenStoreImplForDeletion.class);

    private SecretTokenService secretTokenService;

    public SecretTokenStoreImplForDeletion(SecretTokenService secretTokenService) {
        this.secretTokenService = secretTokenService;
    }

    @Override
    public int deleteByProcessInstanceId(String processInstanceId) {
        int result = secretTokenService.createQuery()
                                       .processInstanceId(processInstanceId)
                                       .delete();
        LOGGER.debug(MessageFormat.format(Messages.DELETED_0_SECRET_TOKENS_FOR_PROCESS_WITH_ID_1, result, processInstanceId));
        return result;
    }

    @Override
    public int deleteOlderThan(LocalDateTime expirationTime) {
        int result = secretTokenService.createQuery()
                                       .olderThan(expirationTime)
                                       .delete();
        LOGGER.debug(MessageFormat.format(Messages.DELETED_0_SECRET_TOKENS_WITH_EXPIRATION_DATE_1, result, expirationTime));
        return result;
    }

}
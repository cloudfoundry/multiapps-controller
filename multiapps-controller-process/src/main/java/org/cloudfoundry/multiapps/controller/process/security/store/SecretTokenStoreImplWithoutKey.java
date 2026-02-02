package org.cloudfoundry.multiapps.controller.process.security.store;

import java.text.MessageFormat;
import java.time.LocalDateTime;

import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.services.SecretTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecretTokenStoreImplWithoutKey implements SecretTokenStoreDeletion {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretTokenStoreImplWithoutKey.class);

    private SecretTokenService secretTokenService;

    public SecretTokenStoreImplWithoutKey(SecretTokenService secretTokenService) {
        this.secretTokenService = secretTokenService;
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

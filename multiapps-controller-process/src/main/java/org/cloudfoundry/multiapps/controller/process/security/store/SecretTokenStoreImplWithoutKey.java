package org.cloudfoundry.multiapps.controller.process.security.store;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDateTime;

import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.services.SecretTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecretTokenStoreImplWithoutKey implements SecretTokenStoreDeletion {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private SecretTokenService secretTokenService;

    public SecretTokenStoreImplWithoutKey(SecretTokenService secretTokenService) {
        this.secretTokenService = secretTokenService;
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

package org.cloudfoundry.multiapps.controller.process.jobs;

import java.text.MessageFormat;
import java.time.LocalDateTime;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.security.store.SecretTokenStoreDeletion;
import org.cloudfoundry.multiapps.controller.process.security.store.SecretTokenStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

@Named
@Order(10)
public class SecretTokenCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretTokenCleaner.class);

    protected final SecretTokenStoreFactory secretTokenStoreFactory;

    @Inject
    public SecretTokenCleaner(SecretTokenStoreFactory secretTokenStoreFactory) {
        this.secretTokenStoreFactory = secretTokenStoreFactory;
    }

    public void execute(LocalDateTime expirationTime) {
        LOGGER.info(CleanUpJob.LOG_MARKER, Messages.REMOVING_EXPIRED_SECRET_TOKENS);

        SecretTokenStoreDeletion secretTokenStore = secretTokenStoreFactory.createSecretTokenStoreDeletionRelated();
        int tokens = secretTokenStore.deleteOlderThan(expirationTime);

        LOGGER.info(CleanUpJob.LOG_MARKER, MessageFormat.format(Messages.REMOVED_SECRET_TOKENS_0, tokens));
    }

}

package org.cloudfoundry.multiapps.controller.process.jobs;

import static java.text.MessageFormat.format;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.persistence.services.AccessTokenService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

@Named
@Order(10)
public class TokensCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokensCleaner.class);

    protected final AccessTokenService accessTokenService;

    @Inject
    public TokensCleaner(AccessTokenService accessTokenService) {
        this.accessTokenService = accessTokenService;
    }

    @Override
    public void execute(Date expirationTime) {
        LocalDateTime date = ZonedDateTime.now()
                                          .toLocalDateTime();
        LOGGER.debug(CleanUpJob.LOG_MARKER, Messages.REMOVING_EXPIRED_TOKENS_FROM_TOKEN_STORE);
        int deletedTokensCount = accessTokenService.createQuery()
                                                   .expiresBefore(date)
                                                   .delete();
        LOGGER.info(CleanUpJob.LOG_MARKER, format(Messages.REMOVED_TOKENS_0, deletedTokensCount));
    }

}

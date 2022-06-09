package com.sap.cloud.lm.sl.cf.process.jobs;

import static java.text.MessageFormat.format;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dao.AccessTokenDao;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Component
@Order(10)
public class TokensCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokensCleaner.class);

    protected final AccessTokenDao accessTokenDao;

    @Inject
    public TokensCleaner(AccessTokenDao accessTokenDao) {
        this.accessTokenDao = accessTokenDao;
    }

    @Override
    public void execute(Date expirationTime) {
        LocalDateTime date = ZonedDateTime.now()
                                          .toLocalDateTime();
        LOGGER.debug(CleanUpJob.LOG_MARKER, Messages.REMOVING_EXPIRED_TOKENS_FROM_TOKEN_STORE);
        int deletedTokensCount = accessTokenDao.deleteTokensWithExpirationBefore(date);
        LOGGER.info(CleanUpJob.LOG_MARKER, format(Messages.REMOVED_TOKENS_0, deletedTokensCount));
    }

}

package com.sap.cloud.lm.sl.cf.web.util;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.InternalAuthenticationServiceException;

import com.sap.cloud.lm.sl.cf.core.dao.AccessTokenDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OrderDirection;
import com.sap.cloud.lm.sl.cf.core.model.AccessToken;
import com.sap.cloud.lm.sl.cf.core.security.token.TokenParserChain;
import com.sap.cloud.lm.sl.cf.core.util.SingleThreadExecutor;
import com.sap.cloud.lm.sl.cf.web.message.Messages;

@Named
public class TokenReuser {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenReuser.class);

    private final AccessTokenDao accessTokenDao;
    private final TokenParserChain tokenParserChain;
    private final SingleThreadExecutor singleThreadExecutor;

    @Inject
    public TokenReuser(AccessTokenDao accessTokenDao, TokenParserChain tokenParserChain, SingleThreadExecutor singleThreadExecutor) {
        this.accessTokenDao = accessTokenDao;
        this.tokenParserChain = tokenParserChain;
        this.singleThreadExecutor = singleThreadExecutor;
    }

    public Optional<OAuth2AccessTokenWithAdditionalInfo> getValidTokenWithExpirationAfterIfPresent(String username,
                                                                                                   long expiresAfterInSeconds) {
        List<AccessToken> accessTokens = accessTokenDao.getTokensByUsernameSortedByExpirationDate(username, OrderDirection.DESCENDING);
        if (accessTokens.isEmpty()) {
            return Optional.empty();
        }
        LocalDateTime dateAfter = calculateDateAfter(expiresAfterInSeconds);
        return accessTokens.stream()
                           .filter(accessToken -> doesTokenExpireAfter(accessToken, dateAfter))
                           .map(this::parseAccessToken)
                           .filter(Optional::isPresent)
                           .map(Optional::get)
                           .findFirst();
    }

    private LocalDateTime calculateDateAfter(long expiresAfterInSeconds) {
        return ZonedDateTime.now()
                            .plus(Duration.ofSeconds(expiresAfterInSeconds))
                            .toLocalDateTime();
    }

    private boolean doesTokenExpireAfter(AccessToken accessToken, LocalDateTime dateAfter) {
        return accessToken.getExpiresAt()
                          .isAfter(dateAfter);
    }

    private Optional<OAuth2AccessTokenWithAdditionalInfo> parseAccessToken(AccessToken accessToken) {
        try {
            return Optional.of(tokenParserChain.parse(new String(accessToken.getValue(), StandardCharsets.UTF_8)));
        } catch (InternalAuthenticationServiceException e) {
            LOGGER.error(e.getMessage(), e);
            LOGGER.debug(MessageFormat.format(Messages.DELETING_TOKEN_WITH_ID_0_AND_EXPIRATION_1, accessToken.getId(),
                                              accessToken.getExpiresAt()));
            singleThreadExecutor.submitTask(() -> accessTokenDao.remove(accessToken));
        }
        return Optional.empty();
    }

}

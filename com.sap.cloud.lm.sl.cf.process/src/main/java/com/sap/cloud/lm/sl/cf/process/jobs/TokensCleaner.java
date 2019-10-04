package com.sap.cloud.lm.sl.cf.process.jobs;

import static java.text.MessageFormat.format;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenStore;

import com.sap.cloud.lm.sl.cf.core.util.SecurityUtil;
import com.sap.cloud.lm.sl.cf.process.message.Messages;

@Named
@Order(10)
public class TokensCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokensCleaner.class);

    protected final TokenStore tokenStore;

    @Inject
    public TokensCleaner(@Named("tokenStore") TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public void execute(Date expirationTime) {
        Collection<OAuth2AccessToken> tokens = tokenStore.findTokensByClientId(SecurityUtil.CLIENT_ID);
        LOGGER.debug(CleanUpJob.LOG_MARKER, Messages.REMOVING_EXPIRED_TOKENS_FROM_TOKEN_STORE);
        int removedTokens = removeTokens(tokens);
        LOGGER.info(CleanUpJob.LOG_MARKER, format(Messages.REMOVED_TOKENS_0, removedTokens));
    }

    private int removeTokens(Collection<OAuth2AccessToken> tokens) {
        List<OAuth2AccessToken> expiredTokens = getExpiredTokens(tokens);
        expiredTokens.forEach(tokenStore::removeAccessToken);
        return expiredTokens.size();
    }

    private List<OAuth2AccessToken> getExpiredTokens(Collection<OAuth2AccessToken> tokens) {
        return tokens.stream()
                     .filter(OAuth2AccessToken::isExpired)
                     .collect(Collectors.toList());
    }

}

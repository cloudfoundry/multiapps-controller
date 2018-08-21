package com.sap.cloud.lm.sl.cf.process.jobs;

import static java.text.MessageFormat.format;

import java.util.Collection;
import java.util.Date;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.util.SecurityUtil;

@Component
@Order(10)
public class TokensCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokensCleaner.class);

    private TokenStore tokenStore;

    @Inject
    public TokensCleaner(@Named("tokenStore") TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public void execute(Date expirationTime) {
        Collection<OAuth2AccessToken> allTokens = tokenStore.findTokensByClientId(SecurityUtil.CLIENT_ID);
        if (CollectionUtils.isEmpty(allTokens)) {
            return;
        }
        LOGGER.info("Removing expired tokens");
        long removedTokens = allTokens.stream()
            .filter(OAuth2AccessToken::isExpired)
            .peek(tokenStore::removeAccessToken)
            .count();
        LOGGER.info(format("Removed expired tokens count: {0}", removedTokens));
    }

}

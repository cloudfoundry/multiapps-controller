package org.cloudfoundry.multiapps.controller.web.security;

import java.text.MessageFormat;

import javax.sql.DataSource;

import org.cloudfoundry.multiapps.controller.web.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;

public class TokenStoreFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenStoreFactory.class);

    private TokenStoreFactory() {
    }

    public static JdbcTokenStore getTokenStore(DataSource dbDataSource) {
        LOGGER.info(MessageFormat.format(Messages.OAUTH_TOKEN_STORE, "JdbcTokenStore"));
        return new JdbcTokenStore(dbDataSource);
    }
}

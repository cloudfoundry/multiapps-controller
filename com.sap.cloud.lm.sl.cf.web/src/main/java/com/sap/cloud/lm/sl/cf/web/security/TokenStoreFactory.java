package com.sap.cloud.lm.sl.cf.web.security;

import java.text.MessageFormat;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;

import com.sap.cloud.lm.sl.cf.core.security.token.store.SingleUserTokenStore;
import com.sap.cloud.lm.sl.cf.web.message.Messages;

public class TokenStoreFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenStoreFactory.class);

    public static JdbcTokenStore getTokenStore(DataSource dbDataSource, DataSource secureStoreDataSource) {
        if (secureStoreDataSource.equals(dbDataSource)) {
            LOGGER.info(MessageFormat.format(Messages.OAUTH_TOKEN_STORE, "SingleUserTokenStore"));
            return new SingleUserTokenStore(dbDataSource);
        }

        LOGGER.info(MessageFormat.format(Messages.OAUTH_TOKEN_STORE, "HanaSecureTokenStore"));
        return new HanaSecureTokenStore(dbDataSource, secureStoreDataSource);
    }
}

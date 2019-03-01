package com.sap.cloud.lm.sl.cf.core.security.token.store;

import java.sql.SQLException;
import java.text.MessageFormat;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.AuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.DefaultAuthenticationKeyGenerator;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;

import com.sap.cloud.lm.sl.cf.client.util.TokenProperties;
import com.sap.cloud.lm.sl.cf.persistence.executors.SqlQueryExecutor;
import com.sap.cloud.lm.sl.cf.persistence.query.providers.SqlOauthTokenQueryProvider;

public class SingleUserTokenStore extends JdbcTokenStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(SingleUserTokenStore.class);

    private final SqlQueryExecutor sqlQueryExecutor;
    private final SqlOauthTokenQueryProvider sqlOauthTokenQueryProvider;
    private final AuthenticationKeyGenerator authenticationKeyGenerator;

    public SingleUserTokenStore(DataSource dataSource) {
        super(dataSource);
        this.sqlQueryExecutor = new SqlQueryExecutor(dataSource);
        this.sqlOauthTokenQueryProvider = new SqlOauthTokenQueryProvider();
        this.authenticationKeyGenerator = new DefaultAuthenticationKeyGenerator();
    }

    public OAuth2Authentication findAuthenticationByUsername(String username) {
        try {
            byte[] serilizedAuthentication = sqlQueryExecutor
                .executeWithAutoCommit(sqlOauthTokenQueryProvider.getFindAuthenticationQuery(username));
            return deserializeAuthentication(serilizedAuthentication);
        } catch (SQLException e) {
            LOGGER.error(MessageFormat.format("Error to get authentication {0}", e.getMessage()));
            return null;
        }
    }

    @Override
    public void storeAccessToken(OAuth2AccessToken token, OAuth2Authentication authentication) {
        String username = TokenProperties.fromToken(token)
            .getUserName();
        String tokenString = extractTokenKey(token.getValue());
        byte[] serializedToken = serializeAccessToken(token);
        String authenticationKey = authenticationKeyGenerator.extractKey(authentication);
        String authenticationClientId = authentication.getOAuth2Request()
            .getClientId();
        byte[] serializedAuthentication = serializeAuthentication(authentication);
        try {
            boolean hasTokenInCache = sqlQueryExecutor.execute(sqlOauthTokenQueryProvider.getSelectUserQuery(username));
            if (hasTokenInCache) {
                sqlQueryExecutor.execute(sqlOauthTokenQueryProvider.getUpdateTokenQuery(tokenString, serializedToken, authenticationKey,
                    authenticationClientId, serializedAuthentication, username));
                return;
            }
            sqlQueryExecutor.execute(sqlOauthTokenQueryProvider.getInsertTokenQuery(tokenString, serializedToken, authenticationKey,
                username, authenticationClientId, serializedAuthentication));
        } catch (SQLException e) {
            LOGGER.error(MessageFormat.format("Error to store token in cache {0}", e.getMessage()));
        }
    }
}

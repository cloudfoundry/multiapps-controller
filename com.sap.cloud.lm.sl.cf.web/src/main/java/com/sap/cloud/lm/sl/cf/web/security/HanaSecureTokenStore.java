package com.sap.cloud.lm.sl.cf.web.security;

import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.CallableStatementCreator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.store.JdbcTokenStore;

import com.sap.cloud.lm.sl.cf.client.util.TokenFactory;
import com.sap.cloud.lm.sl.cf.web.message.Messages;
import com.sap.cloud.lm.sl.cf.web.util.CompressUtil;

public class HanaSecureTokenStore extends JdbcTokenStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(HanaSecureTokenStore.class);

    private static final int TOKEN_KEY_MAX_LENGTH = 32;

    private static final String INSERT_PROCEDURE_NAME = "USER_SECURESTORE_INSERT";
    private static final String RETRIEVE_PROCEDURE_NAME = "USER_SECURESTORE_RETRIEVE";
    private static final String DELETE_PROCEDURE_NAME = "USER_SECURESTORE_DELETE";

    private static final String PROCEDURE_SECURESTORE_INSERT = "{call SYS." + INSERT_PROCEDURE_NAME + "(?, ?, ?, ?)}";
    private static final String PROCEDURE_SECURESTORE_RETRIEVE = "{call SYS." + RETRIEVE_PROCEDURE_NAME + "(?, ?, ?, ?)}";
    private static final String PROCEDURE_SECURESTORE_DELETE = "{call SYS." + DELETE_PROCEDURE_NAME + "(?, ?, ?)}";

    private static final String PROCEDURE_SECURESTORE_INSERT_LEGACY = "{call SYS.USER_SECURESTORE_INSERT_DEV(?, ?, ?, ?)}";
    private static final String PROCEDURE_SECURESTORE_RETRIEVE_LEGACY = "{call SYS.USER_SECURESTORE_RETRIEVE_DEV(?, ?, ?, ?)}";
    private static final String PROCEDURE_SECURESTORE_DELETE_LEGACY = "{call SYS.USER_SECURESTORE_DELETE_DEV(?, ?, ?)}";

    private static final String OAUTH_ACCESS_TOKEN_STORE = "DS_OAUTH_ACCESS_TOKEN_STORE";
    private static final boolean FOR_XS_APPLICATIONUSER = false;

    private final JdbcTemplate jdbcTemplate;

    public HanaSecureTokenStore(DataSource dbDataSource, DataSource secureStoreDataSource) {
        super(dbDataSource);
        this.jdbcTemplate = new JdbcTemplate(secureStoreDataSource);
    }

    @Override
    public void removeAccessToken(String tokenValue) {
        super.removeAccessToken(tokenValue);
        removeAccessTokenFromSecureStore(extractTokenKey(tokenValue));
    }

    @Override
    public void storeRefreshToken(OAuth2RefreshToken refreshToken, OAuth2Authentication authentication) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OAuth2RefreshToken readRefreshToken(String tokenValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OAuth2Authentication readAuthenticationForRefreshToken(OAuth2RefreshToken token) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeRefreshToken(OAuth2RefreshToken token) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAccessTokenUsingRefreshToken(OAuth2RefreshToken refreshToken) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected byte[] serializeAccessToken(OAuth2AccessToken token) {
        String tokenKey = extractTokenKey(token.getValue());
        storeAccessTokenInSecureStore(tokenKey, token);
        return tokenKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected OAuth2AccessToken deserializeAccessToken(byte[] tokenKeyBytes) {
        String tokenKey = new String(tokenKeyBytes, StandardCharsets.UTF_8);
        return readAccessTokenFromSecureStore(tokenKey);
    }

    private void storeAccessTokenInSecureStore(String tokenKey, OAuth2AccessToken token) {
        SqlParameter storeNameParam = new SqlParameter(Types.VARCHAR);
        SqlParameter forXsApplicationUserParam = new SqlParameter(Types.BOOLEAN);
        SqlParameter keyParam = new SqlParameter(Types.VARCHAR);
        SqlParameter valueParam = new SqlParameter(Types.VARBINARY);

        List<SqlParameter> paramList = Arrays.asList(storeNameParam, forXsApplicationUserParam, keyParam, valueParam);

        byte[] serializeAccessToken = token.getValue()
            .getBytes(StandardCharsets.UTF_8);
        byte[] compressedBytes = CompressUtil.compress(serializeAccessToken);

        try {
            callInsert(tokenKey, paramList, compressedBytes, PROCEDURE_SECURESTORE_INSERT);
        } catch (ConcurrencyFailureException e) {
            LOGGER.debug(MessageFormat.format(Messages.ERROR_STORING_OAUTH_TOKEN_IN_SECURE_STORE, tokenKey));
        } catch (BadSqlGrammarException e) {
            throwIfShouldNotIgnore(e, INSERT_PROCEDURE_NAME);
            try {
                callInsert(tokenKey, paramList, compressedBytes, PROCEDURE_SECURESTORE_INSERT_LEGACY);
            } catch (ConcurrencyFailureException f) {
                LOGGER.debug(MessageFormat.format(Messages.ERROR_STORING_OAUTH_TOKEN_IN_SECURE_STORE, tokenKey));
            }
        }
    }

    private void callInsert(String tokenKey, List<SqlParameter> paramList, byte[] compressedBytes, String procedureSecurestoreInsert) {
        jdbcTemplate.call(new CallableStatementCreator() {
            @Override
            public CallableStatement createCallableStatement(Connection connection) throws SQLException {
                CallableStatement callableStatement = connection.prepareCall(procedureSecurestoreInsert);
                callableStatement.setString(1, OAUTH_ACCESS_TOKEN_STORE);
                callableStatement.setBoolean(2, FOR_XS_APPLICATIONUSER);
                callableStatement.setString(3, tokenKey);
                callableStatement.setBytes(4, compressedBytes);
                return callableStatement;
            }
        }, paramList);
    }

    private OAuth2AccessToken readAccessTokenFromSecureStore(String tokenKey) {
        if (tokenKey.length() > TOKEN_KEY_MAX_LENGTH) {
            throw new IllegalArgumentException(Messages.TOKEN_KEY_FORMAT_NOT_VALID);
        }

        SqlParameter storeNameParam = new SqlParameter(Types.VARCHAR);
        SqlParameter forXsApplicationUserParam = new SqlParameter(Types.BOOLEAN);
        SqlParameter keyParam = new SqlParameter(Types.VARCHAR);
        SqlOutParameter valueParam = new SqlOutParameter("VALUE", Types.VARBINARY);

        List<SqlParameter> paramList = Arrays.asList(storeNameParam, forXsApplicationUserParam, keyParam, valueParam);
        Map<String, Object> result = null;
        try {
            result = callRetrieve(tokenKey, paramList, PROCEDURE_SECURESTORE_RETRIEVE);
        } catch (BadSqlGrammarException e) {
            throwIfShouldNotIgnore(e, RETRIEVE_PROCEDURE_NAME);
            result = callRetrieve(tokenKey, paramList, PROCEDURE_SECURESTORE_RETRIEVE_LEGACY);
        }
        byte[] tokenBytes = (byte[]) result.get("VALUE");
        if (tokenBytes == null) {
            throw new IllegalArgumentException(Messages.TOKEN_NOT_FOUND_IN_SECURE_STORE);
        }
        byte[] decompressedBytes = CompressUtil.decompress(tokenBytes);
        OAuth2AccessToken accessToken = new TokenFactory().createToken(new String(decompressedBytes, StandardCharsets.UTF_8));
        return accessToken;
    }

    private Map<String, Object> callRetrieve(String tokenKey, List<SqlParameter> paramList, final String procedureSecurestoreRetrieve) {
        Map<String, Object> result = jdbcTemplate.call(new CallableStatementCreator() {
            @Override
            public CallableStatement createCallableStatement(Connection connection) throws SQLException {
                CallableStatement callableStatement = connection.prepareCall(procedureSecurestoreRetrieve);
                callableStatement.setString(1, OAUTH_ACCESS_TOKEN_STORE);
                callableStatement.setBoolean(2, FOR_XS_APPLICATIONUSER);
                callableStatement.setString(3, tokenKey);
                callableStatement.registerOutParameter(4, Types.VARBINARY);
                return callableStatement;
            }
        }, paramList);
        return result;
    }

    private void removeAccessTokenFromSecureStore(String tokenKey) {
        SqlParameter storeNameParam = new SqlParameter(Types.VARCHAR);
        SqlParameter forXsApplicationUserParam = new SqlParameter(Types.BOOLEAN);
        SqlParameter keyParam = new SqlParameter(Types.VARCHAR);

        List<SqlParameter> paramList = Arrays.asList(storeNameParam, forXsApplicationUserParam, keyParam);
        try {
            callRemove(tokenKey, paramList, PROCEDURE_SECURESTORE_DELETE);
        } catch (BadSqlGrammarException e) {
            throwIfShouldNotIgnore(e, DELETE_PROCEDURE_NAME);
            callRemove(tokenKey, paramList, PROCEDURE_SECURESTORE_DELETE_LEGACY);
        }
    }

    private void callRemove(String tokenKey, List<SqlParameter> paramList, String procedureSecurestoreDelete) {
        jdbcTemplate.call(new CallableStatementCreator() {
            @Override
            public CallableStatement createCallableStatement(Connection connection) throws SQLException {
                CallableStatement callableStatement = connection.prepareCall(procedureSecurestoreDelete);
                callableStatement.setString(1, OAUTH_ACCESS_TOKEN_STORE);
                callableStatement.setBoolean(2, FOR_XS_APPLICATIONUSER);
                callableStatement.setString(3, tokenKey);
                return callableStatement;
            }
        }, paramList);
    }

    /***
     * In case of fall-back to legacy stored procedure names, the errors caused by calling new non-existing procedures should be ignored.
     * The exceptions in this case are similar to :'com.sap.db.jdbc.exceptions.JDBCDriverException: SAP DBTech JDBC: [328]: invalid name of
     * function or procedure: USER_SECURESTORE_INSERT', and are not in current dependency tree.
     */
    private void throwIfShouldNotIgnore(BadSqlGrammarException e, String procedureName) {
        String errorMessage = e.getCause()
            .getMessage();
        if (errorMessage.contains(procedureName)) {
            return;
        }
        throw e;
    }
}

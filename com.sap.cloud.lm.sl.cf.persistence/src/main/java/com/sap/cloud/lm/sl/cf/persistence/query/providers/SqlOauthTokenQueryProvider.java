package com.sap.cloud.lm.sl.cf.persistence.query.providers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.sap.cloud.lm.sl.cf.persistence.query.SqlQuery;
import com.sap.cloud.lm.sl.cf.persistence.util.JdbcUtil;

public class SqlOauthTokenQueryProvider {
    private static final String INSERT_ACCESS_TOKEN_STATEMENT = "insert into oauth_access_token (token_id, token, authentication_id, user_name, client_id, authentication) values (?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_ACCESS_TOKEN_STATEMENT = "update oauth_access_token SET token_id=?, token=?, authentication_id=?, client_id=?, authentication=? where user_name=?";
    private static final String SELECT_TOKENS_BY_USERNAME = "select token_id from oauth_access_token where user_name = ?";
    private static final String SELECT_AUTHENTICATION_BY_USERNAME = "select authentication from oauth_access_token where user_name = ?";

    public SqlQuery<byte[]> getFindAuthenticationQuery(String username) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = connection.prepareStatement(SELECT_AUTHENTICATION_BY_USERNAME);
                statement.setString(1, username);
                resultSet = statement.executeQuery();
                return getAuthenticationBytes(resultSet);
            } finally {
                JdbcUtil.closeQuietly(resultSet);
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    private byte[] getAuthenticationBytes(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            return resultSet.getBytes(1);
        }
        return new byte[0];
    }

    public SqlQuery<Boolean> getSelectUserQuery(String username) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(SELECT_TOKENS_BY_USERNAME);
                statement.setString(1, username);
                return hasTokenInCache(statement.executeQuery());
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    private boolean hasTokenInCache(ResultSet resultSet) throws SQLException {
        String tokenString = null;
        if (resultSet.next()) {
            tokenString = resultSet.getString(1);
        }
        return tokenString != null;
    }

    public SqlQuery<Void> getUpdateTokenQuery(String tokenString, byte[] serializedToken, String authenticationKey,
        String authenticationClientId, byte[] serializedAuthentication, String username) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(UPDATE_ACCESS_TOKEN_STATEMENT);
                statement.setString(1, tokenString);
                statement.setBytes(2, serializedToken);
                statement.setString(3, authenticationKey);
                statement.setString(4, authenticationClientId);
                statement.setBytes(5, serializedAuthentication);
                statement.setString(6, username);
                statement.executeUpdate();
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
            return null;
        };
    }

    public SqlQuery<Void> getInsertTokenQuery(String tokenString, byte[] serializedToken, String authenticationKey, String username,
        String authenticationClientId, byte[] serializedAuthentication) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(INSERT_ACCESS_TOKEN_STATEMENT);
                statement.setString(1, tokenString);
                statement.setBytes(2, serializedToken);
                statement.setString(3, authenticationKey);
                statement.setString(4, username);
                statement.setString(5, authenticationClientId);
                statement.setBytes(6, serializedAuthentication);
                statement.executeUpdate();
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
            return null;
        };
    }
}

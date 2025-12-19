package org.cloudfoundry.multiapps.controller.persistence.query.providers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata;
import org.cloudfoundry.multiapps.controller.persistence.query.SqlQuery;
import org.cloudfoundry.multiapps.controller.persistence.util.JdbcUtil;

public class SqlSecretTokenQueryProvider {

    private final String tableName;

    private final String secretTokenSequence = PersistenceMetadata.SequenceNames.SECRET_TOKEN_SEQUENCE;

    private static final String INSERT_SECRET_TOKEN = "INSERT INTO %s (id, process_instance_id, variable_name, content, key_id, timestamp) VALUES (nextval('%s'), ?, ?, ?, ?, NOW()) RETURNING id";
    private static final String RETRIEVE_SECRET_TOKEN = "SELECT content FROM %s WHERE id = ? AND process_instance_id = ?";
    private static final String DELETE_SECRET_TOKEN = "DELETE FROM %s WHERE process_instance_id = ?";
    private static final String DELETE_OLDER_THAN = "DELETE FROM %s t WHERE t.timestamp < ?";

    public SqlSecretTokenQueryProvider(String tableName) {
        this.tableName = tableName;
    }

    public SqlQuery<Long> insertSecretToken(String processInstanceId, String variableName, byte[] encryptedBase64, String keyId) {
        return (Connection connection) -> {
            PreparedStatement preparedStatement = null;
            try {
                preparedStatement = connection.prepareStatement(getInsertSecretTokenQuery());
                preparedStatement.setString(1, processInstanceId);
                preparedStatement.setString(2, variableName);
                preparedStatement.setBytes(3, encryptedBase64);
                preparedStatement.setString(4, keyId);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getLong(1);
                    }
                    throw new SQLException("INSERT secret_token did not return an id");
                }
            } finally {
                JdbcUtil.closeQuietly(preparedStatement);
            }

        };
    }

    public SqlQuery<byte[]> getSecretToken(String processInstanceId, long id) {
        return (Connection connection) -> {
            PreparedStatement preparedStatement = null;
            try {
                preparedStatement = connection.prepareStatement(getRetrieveSecretTokenQuery());
                preparedStatement.setLong(1, id);
                preparedStatement.setString(2, processInstanceId);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        byte[] resultValue = resultSet.getBytes(1);
                        return resultValue;
                    }
                    return null;
                }
            } finally {
                JdbcUtil.closeQuietly(preparedStatement);
            }
        };
    }

    public SqlQuery<Integer> deleteForProcessInstance(String processInstanceId) {
        return (Connection connection) -> {
            PreparedStatement preparedStatement = null;
            try {
                preparedStatement = connection.prepareStatement(getDeletionSecretTokenQuery());
                preparedStatement.setString(1, processInstanceId);
                return preparedStatement.executeUpdate();
            } finally {
                JdbcUtil.closeQuietly(preparedStatement);
            }
        };
    }

    public SqlQuery<Integer> deleteOlderThan(LocalDateTime expirationTime) {
        return (Connection connection) -> {
            PreparedStatement preparedStatement = null;
            try {
                preparedStatement = connection.prepareStatement(getDeleteSecretTokenOlderThanQuery());
                preparedStatement.setTimestamp(1, Timestamp.valueOf(expirationTime));
                return preparedStatement.executeUpdate();
            } finally {
                JdbcUtil.closeQuietly(preparedStatement);
            }
        };
    }

    private String getInsertSecretTokenQuery() {
        return String.format(INSERT_SECRET_TOKEN, tableName, secretTokenSequence);
    }

    private String getRetrieveSecretTokenQuery() {
        return String.format(RETRIEVE_SECRET_TOKEN, tableName);
    }

    private String getDeletionSecretTokenQuery() {
        return String.format(DELETE_SECRET_TOKEN, tableName);
    }

    private String getDeleteSecretTokenOlderThanQuery() {
        return String.format(DELETE_OLDER_THAN, tableName);
    }

}

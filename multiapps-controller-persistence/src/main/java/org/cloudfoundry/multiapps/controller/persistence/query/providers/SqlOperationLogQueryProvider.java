package org.cloudfoundry.multiapps.controller.persistence.query.providers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.query.SqlQuery;
import org.cloudfoundry.multiapps.controller.persistence.util.JdbcUtil;

public class SqlOperationLogQueryProvider {

    public static final String INSERT_FILE_ATTRIBUTES_AND_CONTENT = "INSERT INTO %s (ID, SPACE, NAMESPACE, MODIFIED, OPERATION_ID, OPERATION_LOG, OPERATION_LOG_NAME) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String ID_COLUMN_LABEL = "id";
    private static final String OPERATION_LOG_COLUMN_LABEL = "operation_log";
    private static final String OPERATION_LOG_NAME_COLUMN_LABEL = "operation_log_name";
    private static final String OPERATION_LOG_MODIFIED_COLUMN_LABEL = "modified";
    private static final String SELECT_LOGS_BY_SPACE_ID_OPERATION_ID_AND_OPERATION_LOG_NAME = "SELECT ID, OPERATION_LOG, OPERATION_LOG_NAME, MODIFIED FROM %s WHERE SPACE=? AND OPERATION_ID=? AND OPERATION_LOG_NAME=? ORDER BY MODIFIED ASC";
    private static final String SELECT_LOGS_BY_SPACE_ID_AND_NAME = "SELECT DISTINCT ID, OPERATION_LOG, OPERATION_LOG_NAME, MODIFIED FROM %s WHERE SPACE=? AND OPERATION_ID=? ORDER BY MODIFIED ASC";
    private final String tableName;

    public SqlOperationLogQueryProvider(String tableName) {
        this.tableName = tableName;
    }

    public SqlQuery<Integer> getStoreLogQuery(OperationLogEntry operationLogEntry) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(getStoreLogQueryString());
                statement.setString(1, operationLogEntry.getId());
                statement.setString(2, operationLogEntry.getSpace());

                if (operationLogEntry.getNamespace() == null) {
                    statement.setNull(3, Types.NULL);
                } else {
                    statement.setString(3, operationLogEntry.getNamespace());
                }

                statement.setTimestamp(4, Timestamp.valueOf(operationLogEntry.getModified()));
                statement.setString(5, operationLogEntry.getOperationId());
                statement.setString(6, operationLogEntry.getOperationLog());
                statement.setString(7, operationLogEntry.getOperationLogName());

                return statement.executeUpdate();
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    public SqlQuery<List<OperationLogEntry>> getListFilesQueryBySpaceAndOperationId(String space, String operationId) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                List<OperationLogEntry> logs = new ArrayList<>();
                statement = connection.prepareStatement(getListFilesBySpaceAndOperationIdQueryString());
                statement.setString(1, space);
                statement.setString(2, operationId);
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    logs.add(getOperationLogEntry(resultSet));
                }
                return logs;
            } finally {
                JdbcUtil.closeQuietly(resultSet);
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    public SqlQuery<List<OperationLogEntry>> getListFilesQueryBySpaceOperationIdAndLogId(String space, String operationId, String logId) {
        return (Connection connection) -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                List<OperationLogEntry> logs = new ArrayList<>();
                statement = connection.prepareStatement(getListFilesBySpaceOperationIdAndLogIdQueryString());
                statement.setString(1, space);
                statement.setString(2, operationId);
                statement.setString(3, logId);
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    logs.add(getOperationLogEntry(resultSet));
                }
                return logs;
            } finally {
                JdbcUtil.closeQuietly(resultSet);
                JdbcUtil.closeQuietly(statement);
            }
        };
    }

    private String getStoreLogQueryString() {
        return String.format(INSERT_FILE_ATTRIBUTES_AND_CONTENT, tableName);
    }

    private String getListFilesBySpaceAndOperationIdQueryString() {
        return String.format(SELECT_LOGS_BY_SPACE_ID_AND_NAME, tableName);
    }

    private String getListFilesBySpaceOperationIdAndLogIdQueryString() {
        return String.format(SELECT_LOGS_BY_SPACE_ID_OPERATION_ID_AND_OPERATION_LOG_NAME, tableName);
    }

    private OperationLogEntry getOperationLogEntry(ResultSet resultSet) throws SQLException {
        return ImmutableOperationLogEntry.builder()
                                         .id(resultSet.getString(ID_COLUMN_LABEL))
                                         .operationLog(resultSet.getString(OPERATION_LOG_COLUMN_LABEL))
                                         .operationLogName(resultSet.getString(OPERATION_LOG_NAME_COLUMN_LABEL))
                                         .modified(resultSet.getTimestamp(OPERATION_LOG_MODIFIED_COLUMN_LABEL)
                                                            .toLocalDateTime())
                                         .build();
    }
}

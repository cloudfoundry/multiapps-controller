package com.sap.cloud.lm.sl.cf.database.migration.client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

import com.sap.cloud.lm.sl.cf.database.migration.executor.type.DatabaseTypeSetter;
import com.sap.cloud.lm.sl.cf.database.migration.executor.type.DatabaseTypeSetterFactory;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.DatabaseTableColumnMetadata;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.DatabaseTableMetadata;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.ImmutableDatabaseTableColumnMetadata;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.ImmutableDatabaseTableMetadata;
import com.sap.cloud.lm.sl.cf.persistence.query.SqlQuery;
import com.sap.cloud.lm.sl.cf.persistence.util.JdbcUtil;
import com.sap.cloud.lm.sl.cf.persistence.util.SqlQueryExecutor;

public class DatabaseQueryClient {
    private final SqlQueryExecutor sqlQueryExecutor;

    public DatabaseQueryClient(SqlQueryExecutor sqlQueryExecutor) {
        this.sqlQueryExecutor = sqlQueryExecutor;
    }

    public long getLastSequenceValue(String sequenceName) throws SQLException {
        return executeWithAutoCommit((connection) -> {
            PreparedStatement preparedStatement = null;
            ResultSet resultSet = null;
            try {
                preparedStatement = prepareStatement(connection, String.format("SELECT last_value FROM %s", sequenceName));
                resultSet = executeQuery(preparedStatement);
                while (resultSet.next()) {
                    return resultSet.getLong(1);
                }
                return 0L;
            } finally {
                closeResultSetQuietly(resultSet);
                closeStatementQuietly(preparedStatement);
            }
        });
    }

    public void updateSequenceInDatabase(String sequenceName, long lastSequenceValue) throws SQLException {
        executeWithAutoCommit((connection) -> {
            PreparedStatement preparedStatement = null;
            try {
                preparedStatement = prepareStatement(connection,
                                                     String.format("SELECT setval('%s', %d, false)", sequenceName, lastSequenceValue));
                executeQuery(preparedStatement);
                return null;
            } finally {
                closeStatementQuietly(preparedStatement);
            }
        });
    }

    public DatabaseTableMetadata extractTableMetadataFromDatabase(String databaseTable) throws SQLException {
        return executeWithAutoCommit((connection) -> {
            PreparedStatement preparedStatement = null;
            ResultSet resultSet = null;
            try {
                preparedStatement = prepareStatement(connection, String.format("SELECT * FROM %s", databaseTable));
                resultSet = executeQuery(preparedStatement);
                return parseDatabaseTableMetadata(databaseTable, resultSet.getMetaData());
            } finally {
                closeResultSetQuietly(resultSet);
                closeStatementQuietly(preparedStatement);
            }
        });
    }

    private DatabaseTableMetadata parseDatabaseTableMetadata(String databaseTable, ResultSetMetaData resultSetMetadata)
        throws SQLException {
        ImmutableDatabaseTableMetadata.Builder tableMetadataBuilder = ImmutableDatabaseTableMetadata.builder()
                                                                                                    .tableName(databaseTable);
        for (int currentColumnIndex = 1; currentColumnIndex <= resultSetMetadata.getColumnCount(); currentColumnIndex++) {
            tableMetadataBuilder.addTableColumnsMetadata(ImmutableDatabaseTableColumnMetadata.builder()
                                                                                             .columnName(resultSetMetadata.getColumnName(currentColumnIndex))
                                                                                             .columnType(resultSetMetadata.getColumnTypeName(currentColumnIndex))
                                                                                             .build());
        }
        return tableMetadataBuilder.build();
    }

    public ResultSet getDataFromDataSource(String databaseTable) throws SQLException {
        return executeWithAutoCommit((connection) -> {
            PreparedStatement preparedStatement = null;
            ResultSet resultSet = null;
            try {
                preparedStatement = prepareStatement(connection, String.format("SELECT * FROM %s", databaseTable));
                resultSet = executeQuery(preparedStatement);
                return resultSet;
            } finally {
                closeResultSetQuietly(resultSet);
                closeStatementQuietly(preparedStatement);
            }
        });
    }

    public void writeDataToDataSource(ResultSet resultSet, String insertQuery, DatabaseTableMetadata sourceTableMetadata)
        throws SQLException {
        execute((connection) -> {
            try {
                while (resultSet.next()) {
                    transferDataToDatasource(connection, insertQuery, sourceTableMetadata, resultSet);
                }
                return null;
            } finally {
                closeResultSetQuietly(resultSet);
            }
        });
    }

    private void transferDataToDatasource(Connection connection, String insertQuery, DatabaseTableMetadata sourceTableMetadata,
                                          ResultSet resultSet)
        throws SQLException {
        PreparedStatement insertStatement = null;
        try {
            insertStatement = prepareStatement(connection, insertQuery);
            populateInsertStatementParameters(insertStatement, sourceTableMetadata.getTableColumnsMetadata(), resultSet);
            insertStatement.executeUpdate();
        } finally {
            closeStatementQuietly(insertStatement);
        }

    }

    private void populateInsertStatementParameters(PreparedStatement insertStatement, List<DatabaseTableColumnMetadata> columnsMetadata,
                                                   ResultSet resultSet)
        throws SQLException {
        for (int columnIndex = 0; columnIndex < columnsMetadata.size(); columnIndex++) {
            setInsertStatementBasedOnType(columnsMetadata.get(columnIndex)
                                                         .getColumnType(),
                                          insertStatement, resultSet, columnIndex + 1);
        }
    }

    private void setInsertStatementBasedOnType(String databaseColumnType, PreparedStatement insertStatement, ResultSet resultSet,
                                               int columnIndex)
        throws SQLException {
        DatabaseTypeSetter databaseTypeSetter = new DatabaseTypeSetterFactory().get(databaseColumnType);
        databaseTypeSetter.setType(columnIndex, insertStatement, resultSet);
    }

    private <T> T executeWithAutoCommit(SqlQuery<T> sqlQuery) throws SQLException {
        return sqlQueryExecutor.executeWithAutoCommit(sqlQuery);
    }

    private <T> T execute(SqlQuery<T> sqlQuery) throws SQLException {
        return sqlQueryExecutor.execute(sqlQuery);
    }

    private PreparedStatement prepareStatement(Connection connection, String statement) throws SQLException {
        return connection.prepareStatement(statement);
    }

    private ResultSet executeQuery(PreparedStatement preparedStatement) throws SQLException {
        return preparedStatement.executeQuery();
    }

    private void closeStatementQuietly(PreparedStatement preparedStatement) {
        JdbcUtil.closeQuietly(preparedStatement);
    }

    private void closeResultSetQuietly(ResultSet resultSet) {
        JdbcUtil.closeQuietly(resultSet);
    }

}

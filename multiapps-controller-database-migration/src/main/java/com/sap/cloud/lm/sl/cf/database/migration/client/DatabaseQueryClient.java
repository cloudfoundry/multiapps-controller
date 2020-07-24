package com.sap.cloud.lm.sl.cf.database.migration.client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.database.migration.executor.type.DatabaseTypeSetter;
import com.sap.cloud.lm.sl.cf.database.migration.executor.type.DatabaseTypeSetterFactory;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.DatabaseTableColumnMetadata;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.DatabaseTableData;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.DatabaseTableRowData;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.ImmutableDatabaseTableColumnMetadata;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.ImmutableDatabaseTableData;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.ImmutableDatabaseTableRowData;
import com.sap.cloud.lm.sl.cf.persistence.util.JdbcUtil;
import com.sap.cloud.lm.sl.cf.persistence.util.SqlQueryExecutor;

public class DatabaseQueryClient {

    private final SqlQueryExecutor sqlQueryExecutor;

    public DatabaseQueryClient(SqlQueryExecutor sqlQueryExecutor) {
        this.sqlQueryExecutor = sqlQueryExecutor;
    }

    public long getLastSequenceValue(String sequenceName) throws SQLException {
        return sqlQueryExecutor.executeWithAutoCommit(connection -> {
            PreparedStatement preparedStatement = null;
            ResultSet resultSet = null;
            try {
                preparedStatement = connection.prepareStatement(String.format("SELECT last_value FROM %s", sequenceName));
                resultSet = preparedStatement.executeQuery();
                while (resultSet.next()) {
                    return resultSet.getLong(1);
                }
                return 0L;
            } finally {
                JdbcUtil.closeQuietly(resultSet);
                JdbcUtil.closeQuietly(preparedStatement);
            }
        });
    }

    public void updateSequence(String sequenceName, long lastSequenceValue) throws SQLException {
        sqlQueryExecutor.executeWithAutoCommit(connection -> {
            PreparedStatement preparedStatement = null;
            try {
                preparedStatement = connection.prepareStatement(String.format("SELECT setval('%s', %d, false)", sequenceName,
                                                                              lastSequenceValue));
                preparedStatement.executeQuery();
                return null;
            } finally {
                JdbcUtil.closeQuietly(preparedStatement);
            }
        });
    }

    public DatabaseTableData extractTableData(String databaseTable) throws SQLException {
        return sqlQueryExecutor.executeWithAutoCommit(connection -> {
            PreparedStatement preparedStatement = null;
            ResultSet resultSet = null;
            try {
                preparedStatement = connection.prepareStatement(String.format("SELECT * FROM %s", databaseTable));
                resultSet = preparedStatement.executeQuery();
                return buildDatabaseTableData(databaseTable, resultSet);
            } finally {
                JdbcUtil.closeQuietly(resultSet);
                JdbcUtil.closeQuietly(preparedStatement);
            }
        });
    }

    private DatabaseTableData buildDatabaseTableData(String databaseTable, ResultSet resultSet) throws SQLException {
        List<DatabaseTableColumnMetadata> databaseTableColumnsMetadata = parseDatabaseColumnsMetadata(resultSet.getMetaData());
        List<DatabaseTableRowData> databaseTableRowsData = parseDatabaseRowsData(resultSet, databaseTableColumnsMetadata);
        return ImmutableDatabaseTableData.builder()
                                         .tableName(databaseTable)
                                         .addAllTableColumnsMetadata(databaseTableColumnsMetadata)
                                         .addAllTableRowsData(databaseTableRowsData)
                                         .build();
    }

    private List<DatabaseTableColumnMetadata> parseDatabaseColumnsMetadata(ResultSetMetaData resultSetMetadata) throws SQLException {
        List<DatabaseTableColumnMetadata> databaseColumnsMetadata = new ArrayList<>();
        for (int currentColumnIndex = 1; currentColumnIndex <= resultSetMetadata.getColumnCount(); currentColumnIndex++) {
            databaseColumnsMetadata.add(ImmutableDatabaseTableColumnMetadata.builder()
                                                                            .columnName(resultSetMetadata.getColumnName(currentColumnIndex))
                                                                            .columnType(resultSetMetadata.getColumnTypeName(currentColumnIndex))
                                                                            .build());
        }
        return databaseColumnsMetadata;
    }

    private List<DatabaseTableRowData> parseDatabaseRowsData(ResultSet resultSet, List<DatabaseTableColumnMetadata> databaseColumnsMetadata)
        throws SQLException {
        List<DatabaseTableRowData> databaseTableRowsData = new ArrayList<>();
        while (resultSet.next()) {
            databaseTableRowsData.add(ImmutableDatabaseTableRowData.builder()
                                                                   .putAllValues(collectRowValues(resultSet, databaseColumnsMetadata))
                                                                   .build());
        }
        return databaseTableRowsData;
    }

    private Map<String, Object> collectRowValues(ResultSet resultSet, List<DatabaseTableColumnMetadata> databaseColumnsMetadata)
        throws SQLException {
        Map<String, Object> databaseTableRowValues = new HashMap<>();
        for (DatabaseTableColumnMetadata databaseColumnMetadata : databaseColumnsMetadata) {
            String columnName = databaseColumnMetadata.getColumnName();
            databaseTableRowValues.put(columnName, resultSet.getObject(columnName));
        }
        return databaseTableRowValues;
    }

    public void writeDataToDataSource(String insertQuery, DatabaseTableData sourceTableData) throws SQLException {
        sqlQueryExecutor.execute(connection -> {
            for (int index = 0; index < sourceTableData.getTableRowsData()
                                                       .size(); index++) {
                DatabaseTableRowData databaseTableRowData = sourceTableData.getTableRowsData()
                                                                           .get(index);
                transferDataToDataSource(connection, insertQuery, sourceTableData.getTableColumnsMetadata(), databaseTableRowData);
            }
            return null;
        });
    }

    private void transferDataToDataSource(Connection connection, String insertQuery,
                                          List<DatabaseTableColumnMetadata> databaseTableColumnsMetadata,
                                          DatabaseTableRowData databaseTableRowData)
        throws SQLException {
        PreparedStatement insertStatement = null;
        try {
            insertStatement = connection.prepareStatement(insertQuery);
            populateInsertStatementParameters(insertStatement, databaseTableColumnsMetadata, databaseTableRowData);
            insertStatement.executeUpdate();
        } finally {
            JdbcUtil.closeQuietly(insertStatement);
        }

    }

    private void populateInsertStatementParameters(PreparedStatement insertStatement,
                                                   List<DatabaseTableColumnMetadata> databaseTableColumnsMetadata,
                                                   DatabaseTableRowData databaseTableRowData)
        throws SQLException {
        for (int columnIndex = 0; columnIndex < databaseTableColumnsMetadata.size(); columnIndex++) {
            DatabaseTableColumnMetadata databaseTableColumnMetadata = databaseTableColumnsMetadata.get(columnIndex);
            setInsertStatementBasedOnType(databaseTableColumnMetadata.getColumnType(), insertStatement, databaseTableRowData.getValues()
                                                                                                                            .get(databaseTableColumnMetadata.getColumnName()),
                                          columnIndex + 1);
        }
    }

    private void setInsertStatementBasedOnType(String databaseColumnType, PreparedStatement insertStatement, Object value, int columnIndex)
        throws SQLException {
        DatabaseTypeSetter databaseTypeSetter = new DatabaseTypeSetterFactory().get(databaseColumnType);
        databaseTypeSetter.setType(columnIndex, insertStatement, value);
    }

}

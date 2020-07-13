package com.sap.cloud.lm.sl.cf.database.migration.executor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

import org.immutables.value.Value.Immutable;

import com.sap.cloud.lm.sl.cf.database.migration.executor.type.DatabaseTypeSetter;
import com.sap.cloud.lm.sl.cf.database.migration.executor.type.DatabaseTypeSetterFactory;
import com.sap.cloud.lm.sl.cf.database.migration.generator.DatabaseTableInsertQueryGenerator;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.DatabaseTableColumnMetadata;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.DatabaseTableData;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.ImmutableDatabaseTableColumnMetadata;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.ImmutableDatabaseTableData;
import com.sap.cloud.lm.sl.cf.persistence.util.JdbcUtil;

@Immutable
public abstract class DatabaseTableMigrationExecutor extends DatabaseMigrationExecutor {

    @Override
    public void executeMigrationInternal(String databaseTable) throws SQLException {
        logger.info("Migrating table \"{}\"...", databaseTable);
        DatabaseTableData sourceTableMetadata = extractTableMetadataFromSourceDatabase(databaseTable);
        transferData(databaseTable, sourceTableMetadata);
    }

    private DatabaseTableData extractTableMetadataFromSourceDatabase(String databaseTable) throws SQLException {
        return getSqlQueryExecutor(getSourceDataSource()).executeWithAutoCommit((connection) -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = connection.prepareStatement(String.format("SELECT * FROM %s", databaseTable));
                resultSet = statement.executeQuery();
                return parseDatabaseTableMetadata(databaseTable, resultSet.getMetaData());
            } finally {
                JdbcUtil.closeQuietly(resultSet);
                JdbcUtil.closeQuietly(statement);
            }
        });
    }

    private DatabaseTableData parseDatabaseTableMetadata(String databaseTable, ResultSetMetaData resultSetMetadata) throws SQLException {
        ImmutableDatabaseTableData.Builder tableMetadataBuilder = ImmutableDatabaseTableData.builder()
                                                                                            .tableName(databaseTable);
        for (int currentColumnIndex = 1; currentColumnIndex <= resultSetMetadata.getColumnCount(); currentColumnIndex++) {
            tableMetadataBuilder.addTableColumnsMetadata(ImmutableDatabaseTableColumnMetadata.builder()
                                                                                             .columnName(resultSetMetadata.getColumnName(currentColumnIndex))
                                                                                             .columnType(resultSetMetadata.getColumnTypeName(currentColumnIndex))
                                                                                             .build());
        }
        return tableMetadataBuilder.build();
    }

    private void transferData(String databaseTable, DatabaseTableData sourceTableMetadata) throws SQLException {
        logger.info("Transfering data for table \"{}\"...", databaseTable);
        String insertQuery = new DatabaseTableInsertQueryGenerator().generate(sourceTableMetadata);
        getSqlQueryExecutor(getSourceDataSource()).executeWithAutoCommit((sourceConnection) -> {
            getSqlQueryExecutor(getTargetDataSource()).execute((targetConnection) -> {
                PreparedStatement statement = null;
                ResultSet resultSet = null;
                try {
                    statement = sourceConnection.prepareStatement(String.format("SELECT * FROM %s", databaseTable));
                    resultSet = statement.executeQuery();
                    while (resultSet.next()) {
                        transferDataToTargetDatasource(targetConnection, insertQuery, sourceTableMetadata, resultSet);
                    }
                    return null;
                } finally {
                    JdbcUtil.closeQuietly(resultSet);
                    JdbcUtil.closeQuietly(statement);
                }
            });
            return null;
        });
    }

    private void transferDataToTargetDatasource(Connection connection, String insertQuery, DatabaseTableData sourceTableMetadata,
                                                ResultSet resultSet)
        throws SQLException {
        PreparedStatement insertStatement = null;
        try {
            insertStatement = connection.prepareStatement(insertQuery);
            populateInsertStatementParameters(insertStatement, sourceTableMetadata.getTableColumnsMetadata(), resultSet);
            insertStatement.executeUpdate();
        } finally {
            JdbcUtil.closeQuietly(insertStatement);
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
}

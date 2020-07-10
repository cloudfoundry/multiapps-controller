package com.sap.cloud.lm.sl.cf.database.migration.executor;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.immutables.value.Value;
import org.immutables.value.Value.Immutable;

import com.sap.cloud.lm.sl.cf.database.migration.generator.DatabaseTableInsertQueryGenerator;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.DatabaseTableMetadata;

@Immutable
public abstract class DatabaseTableMigrationExecutor extends DatabaseMigrationExecutor {

    @Override
    public void executeMigrationInternal(String databaseTable) throws SQLException {
        logger.info("Migrating table \"{}\"...", databaseTable);
        DatabaseTableMetadata sourceTableMetadata = extractTableMetadataFromSourceDatabase(databaseTable);
        transferData(databaseTable, sourceTableMetadata);
    }

    private DatabaseTableMetadata extractTableMetadataFromSourceDatabase(String databaseTable) throws SQLException {
        return getSourceDatabaseQueryClient().extractTableMetadataFromDatabase(databaseTable);
    }

    private void transferData(String databaseTable, DatabaseTableMetadata sourceTableMetadata) throws SQLException {
        logger.info("Transfering data for table \"{}\"...", databaseTable);
        ResultSet resultSet = getDataFromSourceDataSource(databaseTable);
        writeDataToTargetDataSource(sourceTableMetadata, resultSet);
    }

    private ResultSet getDataFromSourceDataSource(String databaseTable) throws SQLException {
        return getSourceDatabaseQueryClient().getDataFromDataSource(databaseTable);
    }

    private void writeDataToTargetDataSource(DatabaseTableMetadata sourceTableMetadata, ResultSet resultSet) throws SQLException {
        String insertQuery = getDatabaseTableInsertQueryGenerator().generate(sourceTableMetadata);
        getTargetDatabaseQueryClient().writeDataToDataSource(resultSet, insertQuery, sourceTableMetadata);
    }

    @Value.Default
    protected DatabaseTableInsertQueryGenerator getDatabaseTableInsertQueryGenerator() {
        return new DatabaseTableInsertQueryGenerator();
    }

}

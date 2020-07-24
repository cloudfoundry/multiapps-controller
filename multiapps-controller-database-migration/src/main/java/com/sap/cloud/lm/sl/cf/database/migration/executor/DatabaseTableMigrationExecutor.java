package com.sap.cloud.lm.sl.cf.database.migration.executor;

import java.sql.SQLException;

import org.immutables.value.Value;

import com.sap.cloud.lm.sl.cf.database.migration.generator.DatabaseTableInsertQueryGenerator;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.DatabaseTableData;

@Value.Immutable
public abstract class DatabaseTableMigrationExecutor extends DatabaseMigrationExecutor {

    @Override
    public void executeMigrationInternal(String databaseTable) throws SQLException {
        logger.info("Migrating table \"{}\"...", databaseTable);
        DatabaseTableData sourceTableData = getSourceDatabaseQueryClient().extractTableData(databaseTable);
        transferData(databaseTable, sourceTableData);
    }

    private void transferData(String databaseTable, DatabaseTableData sourceTableData) throws SQLException {
        logger.info("Transfering data for table \"{}\"...", databaseTable);
        String insertQuery = getDatabaseTableInsertQueryGenerator().generate(sourceTableData);
        getTargetDatabaseQueryClient().writeDataToDataSource(insertQuery, sourceTableData);
    }

    @Value.Default
    protected DatabaseTableInsertQueryGenerator getDatabaseTableInsertQueryGenerator() {
        return new DatabaseTableInsertQueryGenerator();
    }

}

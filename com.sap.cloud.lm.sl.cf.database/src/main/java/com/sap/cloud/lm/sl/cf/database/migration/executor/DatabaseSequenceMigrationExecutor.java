package com.sap.cloud.lm.sl.cf.database.migration.executor;

import java.sql.SQLException;
import java.text.MessageFormat;

import org.immutables.value.Value;

@Value.Immutable
public abstract class DatabaseSequenceMigrationExecutor extends DatabaseMigrationExecutor {

    @Override
    public void executeMigrationInternal(String sequenceName) throws SQLException {
        logger.info("Migrating sequence \"{}\"...", sequenceName);
        long lastSequenceValue = getLastSequenceValue(sequenceName);
        if (lastSequenceValue <= 0) {
            throw new IllegalStateException(MessageFormat.format("Invalid last sequence value: {0}", lastSequenceValue));
        }
        updateSequenceInTargetDatabase(sequenceName, lastSequenceValue);
    }

    private long getLastSequenceValue(String sequenceName) throws SQLException {
        return getSourceDatabaseQueryClient().getLastSequenceValue(sequenceName);
    }

    private void updateSequenceInTargetDatabase(String sequenceName, long lastSequenceValue) throws SQLException {
        getTargetDatabaseQueryClient().updateSequenceInDatabase(sequenceName, lastSequenceValue);
    }
}

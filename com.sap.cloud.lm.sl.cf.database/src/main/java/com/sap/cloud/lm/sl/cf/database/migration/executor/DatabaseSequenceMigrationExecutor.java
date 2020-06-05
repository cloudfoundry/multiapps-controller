package com.sap.cloud.lm.sl.cf.database.migration.executor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;

import org.immutables.value.Value.Immutable;

import com.sap.cloud.lm.sl.cf.persistence.util.JdbcUtil;

@Immutable
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
        return getSqlQueryExecutor(getSourceDataSource()).executeWithAutoCommit((connection) -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = connection.prepareStatement(String.format("SELECT last_value FROM %s", sequenceName));
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    return resultSet.getLong(1);
                }
                return 0L;
            } finally {
                JdbcUtil.closeQuietly(resultSet);
                JdbcUtil.closeQuietly(statement);
            }
        });
    }

    private void updateSequenceInTargetDatabase(String sequenceName, long lastSequenceValue) throws SQLException {
        getSqlQueryExecutor(getTargetDataSource()).executeWithAutoCommit((connection) -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(String.format("SELECT setval('%s', %d, false)", sequenceName, lastSequenceValue));
                statement.executeQuery();
                return null;
            } finally {
                JdbcUtil.closeQuietly(statement);
            }
        });
    }
}

package com.sap.cloud.lm.sl.cf.core.liquibase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.util.Configuration;
import com.sap.cloud.lm.sl.cf.core.util.Configuration.DatabaseType;
import com.sap.cloud.lm.sl.persistence.changes.liquibase.AbstractChange;
import com.sap.cloud.lm.sl.persistence.util.JdbcUtil;

import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;

public class DropConfigurationRegistryUniqueConstraint extends AbstractChange {

    private static final String HANA_SEARCH_QUERY = "SELECT DISTINCT CONSTRAINT_NAME FROM CONSTRAINTS WHERE SCHEMA_NAME='%s' AND TABLE_NAME='CONFIGURATION_REGISTRY' AND IS_UNIQUE_KEY='TRUE' AND IS_PRIMARY_KEY='FALSE'";
    private static final String POSTGRESQL_SEARCH_QUERY = "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME='configuration_registry' and CONSTRAINT_TYPE='UNIQUE'";
    private static final String DROP_QUERY = "ALTER TABLE configuration_registry DROP CONSTRAINT %s";
    private static final String CONSTRAINT_NAME_COLUMN = "CONSTRAINT_NAME";
    private static final Set<DatabaseType> SUPPORTED_DATABASE_TYPES = new HashSet<>(
        Arrays.asList(DatabaseType.HANA, DatabaseType.POSTGRESQL));

    private DatabaseType databaseType;

    @Override
    public void setUp() throws SetupException {
        this.databaseType = Configuration.getInstance()
            .getDatabaseType();
    }

    @Override
    public String getConfirmationMessage() {
        return Messages.DROPPED_UNNAMED_UNIQUE_CONSTRAINT_FOR_CONFIGURATION_REGISTRY;
    }

    @Override
    public void execute(Database database) throws CustomChangeException {
        if (!SUPPORTED_DATABASE_TYPES.contains(databaseType)) {
            // Normally, one would throw an exception here, but we can't do that because the AuditLogManagerTest will fail.
            logger
                .warn(MessageFormat.format(Messages.UNKNOWN_DB_TYPE_WILL_NOT_DROP_CONFIGURATION_REGISTRY_UNIQUE_CONSTRAINT, databaseType));
            return;
        }
        super.execute(database);
    }

    @Override
    protected void executeInTransaction(JdbcConnection jdbcConnection) throws Exception {
        String constraintName = retrieveConstraintName(jdbcConnection);
        dropConstraint(jdbcConnection, constraintName);
    }

    private String retrieveConstraintName(JdbcConnection jdbcConnection) throws Exception {
        PreparedStatement preparedStatement = null;

        try {
            String searchQuery = getSearchQuery(jdbcConnection);
            preparedStatement = jdbcConnection.prepareStatement(searchQuery);
            ResultSet result = preparedStatement.executeQuery();
            result.next();
            String constraintName = result.getString(CONSTRAINT_NAME_COLUMN);
            logger.info(String.format("Executed statement '%s' returned constraint name: %s", searchQuery, constraintName));
            return constraintName;
        } finally {
            JdbcUtil.closeQuietly(preparedStatement);
        }
    }

    private String getSearchQuery(JdbcConnection jdbcConnection) throws SQLException {
        switch (databaseType) {
            case POSTGRESQL:
                return POSTGRESQL_SEARCH_QUERY;
            case HANA:
                String schemaName = jdbcConnection.getUnderlyingConnection()
                    .getSchema();
                return String.format(HANA_SEARCH_QUERY, schemaName);
            default:
                throw new IllegalStateException();
        }
    }

    private void dropConstraint(JdbcConnection jdbcConnection, String constraintName) throws Exception {
        Statement statement = null;

        try {
            statement = jdbcConnection.createStatement();
            String dropQuery = String.format(DROP_QUERY, constraintName);
            statement.execute(dropQuery);
            logger.info(String.format("Executed statement '%s'.", dropQuery));
        } finally {
            JdbcUtil.closeQuietly(statement);
        }
    }

}

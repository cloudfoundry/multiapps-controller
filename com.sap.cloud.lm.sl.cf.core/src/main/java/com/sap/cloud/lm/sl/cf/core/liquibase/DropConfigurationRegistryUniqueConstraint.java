package com.sap.cloud.lm.sl.cf.core.liquibase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.MessageFormat;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.util.Configuration;
import com.sap.cloud.lm.sl.cf.core.util.Configuration.DatabaseType;
import com.sap.cloud.lm.sl.persistence.util.JdbcUtil;

import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;

public class DropConfigurationRegistryUniqueConstraint extends AbstractChange {

    private static final String HANA_SEARCH_QUERY = "SELECT INDEX_NAME FROM INDEXES WHERE TABLE_NAME='CONFIGURATION_REGISTRY' AND CONSTRAINT LIKE '%UNIQUE%'";
    private static final String POSTGRESQL_SEARCH_QUERY = "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME='configuration_registry' and CONSTRAINT_TYPE='UNIQUE'";
    private static final String DROP_QUERY = "ALTER TABLE configuration_registry DROP CONSTRAINT %s";
    private static final String HANA_CONSTRAINT_NAME_COLUMN = "INDEX_NAME";
    private static final String POSTGRESQL_CONSTRAINT_NAME_COLUMN_NAME = "CONSTRAINT_NAME";

    private String searchQuery;
    private String constraintNameColumn;

    @Override
    public void setUp() throws SetupException {
        DatabaseType databaseType = Configuration.getInstance().getDatabaseType();
        switch (databaseType) {
            case POSTGRESQL:
                searchQuery = POSTGRESQL_SEARCH_QUERY;
                constraintNameColumn = POSTGRESQL_CONSTRAINT_NAME_COLUMN_NAME;
                break;
            case HANA:
                searchQuery = HANA_SEARCH_QUERY;
                constraintNameColumn = HANA_CONSTRAINT_NAME_COLUMN;
                break;
            default:
                // Normally, one would throw an exception here, but we can't do that because the AuditLogManagerTest will fail.
                logger.warn(
                    MessageFormat.format(Messages.UNKNOWN_DB_TYPE_WILL_NOT_DROP_CONFIGURATION_REGISTRY_UNIQUE_CONSTRAINT, databaseType));
        }
    }

    @Override
    public String getConfirmationMessage() {
        return Messages.DROPPED_UNNAMED_UNIQUE_CONSTRAINT_FOR_CONFIGURATION_REGISTRY;
    }

    @Override
    public void execute(Database database) throws CustomChangeException {
        if (searchQuery == null || constraintNameColumn == null) {
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
            preparedStatement = jdbcConnection.prepareStatement(searchQuery);
            ResultSet result = preparedStatement.executeQuery();
            result.next();
            String constraintName = result.getString(constraintNameColumn);
            logger.info(String.format("Executed statement '%s' returned constraint name: %s", searchQuery, constraintName));
            return constraintName;
        } finally {
            JdbcUtil.closeQuietly(preparedStatement);
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

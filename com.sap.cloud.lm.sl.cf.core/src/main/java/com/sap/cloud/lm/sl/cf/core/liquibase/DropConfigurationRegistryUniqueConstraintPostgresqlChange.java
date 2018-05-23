package com.sap.cloud.lm.sl.cf.core.liquibase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.persistence.changes.liquibase.AbstractChange;
import com.sap.cloud.lm.sl.persistence.util.JdbcUtil;

import liquibase.database.jvm.JdbcConnection;

public class DropConfigurationRegistryUniqueConstraintPostgresqlChange extends AbstractChange {

    private static final String POSTGRESQL_SEARCH_QUERY = "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME='configuration_registry' and CONSTRAINT_TYPE='UNIQUE'";
    private static final String DROP_QUERY = "ALTER TABLE configuration_registry DROP CONSTRAINT %s";
    private static final String CONSTRAINT_NAME_COLUMN = "CONSTRAINT_NAME";

    @Override
    public String getConfirmationMessage() {
        return Messages.DROPPED_UNNAMED_UNIQUE_CONSTRAINT_FOR_CONFIGURATION_REGISTRY;
    }

    @Override
    protected void executeInTransaction(JdbcConnection jdbcConnection) throws Exception {
        String constraintName = retrieveConstraintName(jdbcConnection);
        dropConstraint(jdbcConnection, constraintName);
    }

    private String retrieveConstraintName(JdbcConnection jdbcConnection) throws Exception {
        PreparedStatement preparedStatement = null;
        ResultSet result = null;

        try {
            String searchQuery = getSearchQuery(jdbcConnection);
            preparedStatement = jdbcConnection.prepareStatement(searchQuery);
            result = preparedStatement.executeQuery();
            result.next();
            String constraintName = result.getString(CONSTRAINT_NAME_COLUMN);
            logger.info(String.format("Executed statement '%s' returned constraint name: %s", searchQuery, constraintName));
            return constraintName;
        } finally {
            JdbcUtil.closeQuietly(result);
            JdbcUtil.closeQuietly(preparedStatement);
        }
    }

    protected String getSearchQuery(JdbcConnection jdbcConnection) throws SQLException {
        return POSTGRESQL_SEARCH_QUERY;
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

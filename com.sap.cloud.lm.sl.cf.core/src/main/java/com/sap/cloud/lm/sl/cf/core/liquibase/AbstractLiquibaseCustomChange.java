package com.sap.cloud.lm.sl.cf.core.liquibase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public abstract class AbstractLiquibaseCustomChange implements CustomTaskChange{
    
    @Override
    public String getConfirmationMessage() {
        return null;
    }

    @Override
    public void setFileOpener(ResourceAccessor arg0) {
    }

    @Override
    public void setUp() throws SetupException {
    }

    @Override
    public ValidationErrors validate(Database arg0) {
        return null;
    }
    
    @Override
    public void execute(Database arg0) throws CustomChangeException {
        getLogger().debug("Begin spliting");
        final JdbcConnection jdbcConnection = (JdbcConnection) arg0.getConnection();
        try {
            jdbcConnection.setAutoCommit(false);
            Map<Long, String> retrievedData = retrieveData(jdbcConnection);
            Map<Long, String> transformedData = transformData(retrievedData);
            updateTable(jdbcConnection, transformedData);
            jdbcConnection.commit();

        } catch (DatabaseException exception) {
            CustomChangeException error = new CustomChangeException(exception);
            try {
                jdbcConnection.rollback();
            } catch (DatabaseException e) {
                error = new CustomChangeException(e);
            }
            throw error;
        } catch (Exception e) {
            throw new CustomChangeException(e);
        } finally {
            try {
                jdbcConnection.setAutoCommit(true);
            } catch (DatabaseException exception) {
                throw new CustomChangeException(exception);
            }
        }
    }
    
    public Map<Long, String>  retrieveData(final JdbcConnection jdbcConnection) throws CustomChangeException{
        Map<Long, String> result = new HashMap<Long, String>();
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = jdbcConnection.prepareStatement(getSearchQuery());
            ResultSet query = preparedStatement.executeQuery();
            result = customExtractData(query);
            query.close();
            getLogger().debug(String.format("Executed select for table '%s' returned '%s' entries", getTableName(), result.size()));
        } catch (DatabaseException exception) {
            getLogger().debug("Failed with database exception while execute select for table " + getTableName());
            throw new CustomChangeException(exception);
        } catch (SQLException exception) {
            getLogger().debug("Failed with sqlexception exception execute fetch for table " + getTableName());
            throw new CustomChangeException(exception);
        } finally {
            if (preparedStatement != null) {
                try {
                    preparedStatement.close();
                } catch (SQLException exception) {
                    throw new CustomChangeException(exception);
                }
            }
        }
        return result;
    }
    
    public void updateTable(final JdbcConnection jdbcConnection,  Map<Long, String> transformedData)throws CustomChangeException{
        
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = jdbcConnection.prepareStatement(getUpdateQuery());
            for (Map.Entry<Long, String> entry : transformedData.entrySet()) {
                customUpdate(preparedStatement, entry);
            }
            preparedStatement.executeBatch();
            getLogger().debug(String.format("Executed batch update for table '%s' and entries size '%s'", getTableName(), transformedData.size()));
        } catch (DatabaseException exception) {
            throw new CustomChangeException(exception);
        } catch (SQLException exception) {
            throw new CustomChangeException(exception);
        }
    }
    
    public abstract Map<Long, String> transformData(Map<Long, String> retrievedData);

    public abstract Map<Long, String> customExtractData(ResultSet query)throws CustomChangeException, DatabaseException, SQLException;
    
    public abstract String getTableName();
    public abstract Logger getLogger();
    public abstract String getSearchQuery();
    public abstract String getUpdateQuery();
    public abstract void customUpdate(PreparedStatement preparedStatement, Map.Entry<Long, String> entry) throws SQLException;
}

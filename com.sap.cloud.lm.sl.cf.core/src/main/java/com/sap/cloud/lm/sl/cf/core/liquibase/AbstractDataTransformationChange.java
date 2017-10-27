package com.sap.cloud.lm.sl.cf.core.liquibase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.sap.cloud.lm.sl.persistence.util.JdbcUtil;

import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;

public abstract class AbstractDataTransformationChange extends AbstractChange {

    @Override
    protected void executeInTransaction(JdbcConnection jdbcConnection) throws Exception {
        Map<Long, String> retrievedData = retrieveData(jdbcConnection);
        Map<Long, String> transformedData = transformData(retrievedData);
        updateTable(jdbcConnection, transformedData);
    }

    public Map<Long, String> retrieveData(JdbcConnection jdbcConnection) throws CustomChangeException, DatabaseException, SQLException {
        Map<Long, String> result = new HashMap<Long, String>();
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = jdbcConnection.prepareStatement(getSearchQuery());
            ResultSet query = preparedStatement.executeQuery();
            result = customExtractData(query);
            query.close();
            logger.debug(String.format("Executed select for table '%s' returned '%s' entries", getTableName(), result.size()));
        } finally {
            JdbcUtil.closeQuietly(preparedStatement);
        }
        return result;
    }

    public void updateTable(JdbcConnection jdbcConnection, Map<Long, String> transformedData) throws DatabaseException, SQLException {
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = jdbcConnection.prepareStatement(getUpdateQuery());
            for (Map.Entry<Long, String> entry : transformedData.entrySet()) {
                customUpdate(preparedStatement, entry);
            }
            preparedStatement.executeBatch();
            logger.debug(
                String.format("Executed batch update for table '%s' and entries size '%s'", getTableName(), transformedData.size()));
        } finally {
            JdbcUtil.closeQuietly(preparedStatement);
        }
    }

    public abstract Map<Long, String> transformData(Map<Long, String> retrievedData);

    public abstract Map<Long, String> customExtractData(ResultSet query) throws CustomChangeException, DatabaseException, SQLException;

    public abstract String getTableName();

    public abstract String getSearchQuery();

    public abstract String getUpdateQuery();

    public abstract void customUpdate(PreparedStatement preparedStatement, Map.Entry<Long, String> entry) throws SQLException;

}

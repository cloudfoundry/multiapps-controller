package com.sap.cloud.lm.sl.cf.core.liquibase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.sap.cloud.lm.sl.persistence.util.JdbcUtil;

import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;

public abstract class AbstractDataTransformationChange<OriginalDataType, TransformedDataType> extends AbstractChange {

    @Override
    protected void executeInTransaction(JdbcConnection jdbcConnection) throws Exception {
        OriginalDataType retrievedData = retrieveData(jdbcConnection);
        TransformedDataType transformedData = transformData(retrievedData);
        alterTable(jdbcConnection);
        updateTable(jdbcConnection, transformedData);
    }

    public OriginalDataType retrieveData(JdbcConnection jdbcConnection) throws DatabaseException, SQLException {
        PreparedStatement preparedStatement = null;
        OriginalDataType result = null;

        try {
            preparedStatement = jdbcConnection.prepareStatement(getSelectStatement());
            ResultSet resultSet = preparedStatement.executeQuery();
            result = extractData(resultSet);
            resultSet.close();
        } finally {
            JdbcUtil.closeQuietly(preparedStatement);
        }
        return result;
    }

    public void updateTable(JdbcConnection jdbcConnection, TransformedDataType transformedData) throws DatabaseException, SQLException {
        PreparedStatement preparedStatement = null;

        try {
            preparedStatement = jdbcConnection.prepareStatement(getUpdateStatement());
            setUpdateStatementParameters(preparedStatement, transformedData);
            preparedStatement.executeBatch();
        } finally {
            JdbcUtil.closeQuietly(preparedStatement);
        }
    }

    public void alterTable(JdbcConnection jdbcConnection) throws DatabaseException, SQLException {
        for (String alterStatement : getAlterStatements()) {
            executeStatement(jdbcConnection, alterStatement);
        }
    }

    private void executeStatement(JdbcConnection jdbcConnection, String statement) throws DatabaseException, SQLException {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = jdbcConnection.prepareStatement(statement);
            preparedStatement.execute();
        } finally {
            JdbcUtil.closeQuietly(preparedStatement);
        }
    }

    public abstract TransformedDataType transformData(OriginalDataType retrievedData);

    public abstract OriginalDataType extractData(ResultSet resultSet) throws SQLException;

    public abstract String getSelectStatement();

    public abstract String getUpdateStatement();
    
    public String[] getAlterStatements() {
        return new String[0];
    }

    public abstract void setUpdateStatementParameters(PreparedStatement preparedStatement, TransformedDataType transformedData)
        throws SQLException;

}

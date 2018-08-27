package com.sap.cloud.lm.sl.cf.persistence.services;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.sap.cloud.lm.sl.cf.persistence.util.JdbcUtil;

public class SqlExecutor {

    private DataSource dataSource;

    public SqlExecutor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public <T> T execute(StatementExecutor<T> statementExecutor) throws SQLException {
        Connection connection = dataSource.getConnection();
        try {
            T result = statementExecutor.execute(connection);
            JdbcUtil.commit(connection);
            return result;
        } catch (SQLException e) {
            JdbcUtil.rollback(connection);
            throw e;
        } finally {
            JdbcUtil.closeQuietly(connection);
        }
    }

    public <T> T executeInSingleTransaction(StatementExecutor<T> statementExecutor) throws SQLException {
        Connection connection = dataSource.getConnection();
        try {
            connection.setAutoCommit(false);
            T result = statementExecutor.execute(connection);
            JdbcUtil.commit(connection);
            return result;
        } catch (SQLException e) {
            JdbcUtil.rollback(connection);
            throw e;
        } finally {
            connection.setAutoCommit(true);
            JdbcUtil.closeQuietly(connection);
        }
    }

    public interface StatementExecutor<T> {

        T execute(Connection connection) throws SQLException;

    }

}

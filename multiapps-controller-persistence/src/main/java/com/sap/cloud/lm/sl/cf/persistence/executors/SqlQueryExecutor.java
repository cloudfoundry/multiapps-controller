package com.sap.cloud.lm.sl.cf.persistence.executors;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.sap.cloud.lm.sl.cf.persistence.query.SqlQuery;
import com.sap.cloud.lm.sl.cf.persistence.util.JdbcUtil;

public class SqlQueryExecutor {

    private DataSource dataSource;

    public SqlQueryExecutor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public <R> R executeWithAutoCommit(SqlQuery<R> sqlQuery) throws SQLException {
        Connection connection = dataSource.getConnection();
        try {
            connection.setAutoCommit(true);
            return sqlQuery.execute(connection);
        } catch (SQLException e) {
            JdbcUtil.rollback(connection);
            throw e;
        } finally {
            JdbcUtil.closeQuietly(connection);
        }
    }

    public <R> R execute(SqlQuery<R> sqlQuery) throws SQLException {
        Connection connection = dataSource.getConnection();
        try {
            connection.setAutoCommit(false);
            R result = sqlQuery.execute(connection);
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

}

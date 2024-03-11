package com.sap.cloud.lm.sl.cf.persistence.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.persistence.message.Messages;

public class JdbcUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcUtil.class);

    private JdbcUtil() {
        // only static members
    }

    public static void closeQuietly(ResultSet resultSet) {
        if (resultSet == null) {
            return;
        }
        try {
            resultSet.close();
        } catch (SQLException e) {
            logSQLException(e);
            LOGGER.warn(Messages.COULD_NOT_CLOSE_RESULT_SET, e);
        } catch (Exception e) {
            LOGGER.warn(Messages.COULD_NOT_CLOSE_RESULT_SET, e);
        }
    }

    public static void closeQuietly(Statement statement) {
        if (statement == null) {
            return;
        }
        try {
            statement.close();
        } catch (SQLException e) {
            logSQLException(e);
            LOGGER.warn(Messages.COULD_NOT_CLOSE_STATEMENT, e);
        } catch (Exception e) {
            LOGGER.warn(Messages.COULD_NOT_CLOSE_STATEMENT, e);
        }
    }

    public static void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException e) {
            logSQLException(e);
            LOGGER.warn(Messages.COULD_NOT_CLOSE_CONNECTION, e);
        } catch (Exception e) {
            LOGGER.warn(Messages.COULD_NOT_CLOSE_CONNECTION, e);
        }
    }

    public static void logSQLException(SQLException exception) {
        for (Throwable sameLevelException : exception) {
            LOGGER.warn(sameLevelException.getMessage(), sameLevelException);
        }
    }

    public static void commit(Connection connection) throws SQLException {
        if (!connection.getAutoCommit()) {
            connection.commit();
        }
    }

    public static void rollback(Connection connection) throws SQLException {
        if (!connection.getAutoCommit()) {
            connection.rollback();
        }
    }

}

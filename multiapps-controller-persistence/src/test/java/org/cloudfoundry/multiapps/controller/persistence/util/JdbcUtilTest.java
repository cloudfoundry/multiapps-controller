package org.cloudfoundry.multiapps.controller.persistence.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class JdbcUtilTest {

    @Test
    void closeQuietlyResultSet() throws SQLException {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        JdbcUtil.closeQuietly(resultSet);
        Mockito.verify(resultSet)
               .close();
    }

    @Test
    void closeQuietlyResultSetWithException() throws SQLException {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Mockito.doThrow(new SQLException())
               .when(resultSet)
               .close();
        JdbcUtil.closeQuietly(resultSet);
        Mockito.verify(resultSet)
               .close();
    }

    @Test
    void closeQuietlyResultSetWithNull() {
        ResultSet resultSet = null;
        Assertions.assertDoesNotThrow(() -> JdbcUtil.closeQuietly(resultSet));
    }

    @Test
    void closeQuietlyStatement() throws SQLException {
        Statement statement = Mockito.mock(Statement.class);
        JdbcUtil.closeQuietly(statement);
        Mockito.verify(statement)
               .close();
    }

    @Test
    void closeQuietlyStatementWithException() throws SQLException {
        Statement statement = Mockito.mock(Statement.class);
        Mockito.doThrow(new SQLException())
               .when(statement)
               .close();
        JdbcUtil.closeQuietly(statement);
        Mockito.verify(statement)
               .close();
    }

    @Test
    void closeQuietlyStatementWithNull() {
        Statement statement = null;
        Assertions.assertDoesNotThrow(() -> JdbcUtil.closeQuietly(statement));
    }

    @Test
    void closeQuietlyConnection() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        JdbcUtil.closeQuietly(connection);
        Mockito.verify(connection)
               .close();
    }

    @Test
    void closeQuietlyConnectionWithException() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.doThrow(new SQLException())
               .when(connection)
               .close();
        JdbcUtil.closeQuietly(connection);
        Mockito.verify(connection)
               .close();
    }

    @Test
    void closeQuietlyConnectionWithNull() {
        Connection connection = null;
        Assertions.assertDoesNotThrow(() -> JdbcUtil.closeQuietly(connection));
    }

    @Test
    void rollback() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit())
               .thenReturn(false);
        JdbcUtil.rollback(connection);
        Mockito.verify(connection)
               .rollback();
    }

    @Test
    void rollbackWithNull() throws SQLException {
        JdbcUtil.rollback(null);
    }

    @Test
    void rollbackWithAutoCommit() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit())
               .thenReturn(true);
        JdbcUtil.rollback(connection);
        Mockito.verify(connection, Mockito.never())
               .rollback();
    }

    @Test
    void commit() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit())
               .thenReturn(false);
        JdbcUtil.commit(connection);
        Mockito.verify(connection)
               .commit();
    }

    @Test
    void commitWithAutoCommit() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit())
               .thenReturn(true);
        JdbcUtil.commit(connection);
        Mockito.verify(connection, Mockito.never())
               .commit();
    }

}

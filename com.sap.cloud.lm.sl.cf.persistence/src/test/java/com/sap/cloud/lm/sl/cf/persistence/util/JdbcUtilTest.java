package com.sap.cloud.lm.sl.cf.persistence.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class JdbcUtilTest {

    @Test
    public void closeQuietlyResultSet() throws SQLException {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        JdbcUtil.closeQuietly(resultSet);
        Mockito.verify(resultSet)
               .close();
    }

    @Test
    public void closeQuietlyResultSetWithException() throws SQLException {
        ResultSet resultSet = Mockito.mock(ResultSet.class);
        Mockito.doThrow(new SQLException())
               .when(resultSet)
               .close();
        JdbcUtil.closeQuietly(resultSet);
        Mockito.verify(resultSet)
               .close();
    }

    @Test
    public void closeQuietlyResultSetWithNull() {
        ResultSet resultSet = null;
        JdbcUtil.closeQuietly(resultSet);
    }

    @Test
    public void closeQuietlyStatement() throws SQLException {
        Statement statement = Mockito.mock(Statement.class);
        JdbcUtil.closeQuietly(statement);
        Mockito.verify(statement)
               .close();
    }

    @Test
    public void closeQuietlyStatementWithException() throws SQLException {
        Statement statement = Mockito.mock(Statement.class);
        Mockito.doThrow(new SQLException())
               .when(statement)
               .close();
        JdbcUtil.closeQuietly(statement);
        Mockito.verify(statement)
               .close();
    }

    @Test
    public void closeQuietlyStatementWithNull() {
        Statement statement = null;
        JdbcUtil.closeQuietly(statement);
    }

    @Test
    public void closeQuietlyConnection() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        JdbcUtil.closeQuietly(connection);
        Mockito.verify(connection)
               .close();
    }

    @Test
    public void closeQuietlyConnectionWithException() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.doThrow(new SQLException())
               .when(connection)
               .close();
        JdbcUtil.closeQuietly(connection);
        Mockito.verify(connection)
               .close();
    }

    @Test
    public void closeQuietlyConnectionWithNull() {
        Connection connection = null;
        JdbcUtil.closeQuietly(connection);
    }

    @Test
    public void rollback() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit())
               .thenReturn(false);
        JdbcUtil.rollback(connection);
        Mockito.verify(connection)
               .rollback();
    }

    @Test
    public void rollbackWithNull() throws SQLException {
        JdbcUtil.rollback(null);
    }

    @Test
    public void rollbackWithAutoCommit() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit())
               .thenReturn(true);
        JdbcUtil.rollback(connection);
        Mockito.verify(connection, Mockito.never())
               .rollback();
    }

    @Test
    public void commit() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit())
               .thenReturn(false);
        JdbcUtil.commit(connection);
        Mockito.verify(connection)
               .commit();
    }

    @Test
    public void commitWithAutoCommit() throws SQLException {
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(connection.getAutoCommit())
               .thenReturn(true);
        JdbcUtil.commit(connection);
        Mockito.verify(connection, Mockito.never())
               .commit();
    }

}

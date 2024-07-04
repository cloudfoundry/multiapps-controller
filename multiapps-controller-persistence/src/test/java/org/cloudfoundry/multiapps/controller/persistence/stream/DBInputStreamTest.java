package org.cloudfoundry.multiapps.controller.persistence.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.cloudfoundry.multiapps.common.SLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class DBInputStreamTest {

    @Mock
    private InputStream inputStream;
    @Mock
    private PreparedStatement preparedStatement;
    @Mock
    private ResultSet resultSet;
    @Mock
    private Connection connection;

    private DBInputStream dbInputStream;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        dbInputStream = new DBInputStream(inputStream, preparedStatement, resultSet, connection);
    }

    @Test
    void close() throws IOException, SQLException {
        dbInputStream.close();
        verify(preparedStatement).close();
        verify(resultSet).close();
        verify(connection).close();
        verify(connection).commit();
    }

    @Test
    void closeWhenExceptionThrown() throws SQLException {
        doThrow(new SQLException("Cannot fetch input stream")).when(connection)
                                                              .commit();
        Exception exception = assertThrows(SLException.class, () -> dbInputStream.close());
        assertEquals("Cannot fetch input stream", exception.getMessage());
        verify(preparedStatement).close();
        verify(resultSet).close();
        verify(connection).close();
    }
}

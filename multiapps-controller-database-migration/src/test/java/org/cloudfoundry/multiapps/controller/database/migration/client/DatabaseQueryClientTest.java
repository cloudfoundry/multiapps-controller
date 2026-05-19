package org.cloudfoundry.multiapps.controller.database.migration.client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

import org.cloudfoundry.multiapps.controller.database.migration.metadata.DatabaseTableColumnMetadata;
import org.cloudfoundry.multiapps.controller.database.migration.metadata.DatabaseTableData;
import org.cloudfoundry.multiapps.controller.database.migration.metadata.DatabaseTableRowData;
import org.cloudfoundry.multiapps.controller.database.migration.metadata.ImmutableDatabaseTableColumnMetadata;
import org.cloudfoundry.multiapps.controller.database.migration.metadata.ImmutableDatabaseTableData;
import org.cloudfoundry.multiapps.controller.database.migration.metadata.ImmutableDatabaseTableRowData;
import org.cloudfoundry.multiapps.controller.persistence.query.SqlQuery;
import org.cloudfoundry.multiapps.controller.persistence.util.SqlQueryExecutor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class DatabaseQueryClientTest {

    private static final String TEST_SEQUENCE = "test_seq";
    private static final String TEST_TABLE = "test_table";

    @Mock
    private SqlQueryExecutor sqlQueryExecutor;
    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement preparedStatement;
    @Mock
    private ResultSet resultSet;
    @Mock
    private ResultSetMetaData resultSetMetaData;

    private DatabaseQueryClient databaseQueryClient;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        databaseQueryClient = new DatabaseQueryClient(sqlQueryExecutor);
    }

    @Test
    void testGetLastSequenceValueReturnsValueWhenResultSetHasRow() throws SQLException {
        Mockito.when(connection.prepareStatement(Mockito.contains(TEST_SEQUENCE)))
               .thenReturn(preparedStatement);
        Mockito.when(preparedStatement.executeQuery())
               .thenReturn(resultSet);
        Mockito.when(resultSet.next())
               .thenReturn(true);
        Mockito.when(resultSet.getLong(1))
               .thenReturn(42L);
        Mockito.when(sqlQueryExecutor.executeWithAutoCommit(Mockito.<SqlQuery<Long>> any()))
               .thenAnswer(invocation -> invocation.<SqlQuery<Long>> getArgument(0)
                                                   .execute(connection));

        long result = databaseQueryClient.getLastSequenceValue(TEST_SEQUENCE);

        Assertions.assertEquals(42L, result);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(connection)
               .prepareStatement(sqlCaptor.capture());
        Assertions.assertTrue(sqlCaptor.getValue()
                                       .contains(TEST_SEQUENCE));
    }

    @Test
    void testGetLastSequenceValueReturnsZeroWhenResultSetIsEmpty() throws SQLException {
        Mockito.when(connection.prepareStatement(Mockito.anyString()))
               .thenReturn(preparedStatement);
        Mockito.when(preparedStatement.executeQuery())
               .thenReturn(resultSet);
        Mockito.when(resultSet.next())
               .thenReturn(false);
        Mockito.when(sqlQueryExecutor.executeWithAutoCommit(Mockito.<SqlQuery<Long>> any()))
               .thenAnswer(invocation -> invocation.<SqlQuery<Long>> getArgument(0)
                                                   .execute(connection));

        long result = databaseQueryClient.getLastSequenceValue(TEST_SEQUENCE);

        Assertions.assertEquals(0L, result);
    }

    @Test
    void testUpdateSequenceExecutesSetvalQuery() throws SQLException {
        Mockito.when(connection.prepareStatement(Mockito.anyString()))
               .thenReturn(preparedStatement);
        Mockito.when(sqlQueryExecutor.executeWithAutoCommit(Mockito.<SqlQuery<Object>> any()))
               .thenAnswer(invocation -> invocation.<SqlQuery<Object>> getArgument(0)
                                                   .execute(connection));

        databaseQueryClient.updateSequence(TEST_SEQUENCE, 100L);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(connection)
               .prepareStatement(sqlCaptor.capture());
        Assertions.assertTrue(sqlCaptor.getValue()
                                       .contains("setval"));
        Assertions.assertTrue(sqlCaptor.getValue()
                                       .contains(TEST_SEQUENCE));
        Assertions.assertTrue(sqlCaptor.getValue()
                                       .contains("100"));
        Mockito.verify(preparedStatement)
               .executeQuery();
    }

    @Test
    void testExtractTableDataBuildsTableDataFromResultSet() throws SQLException {
        Mockito.when(connection.prepareStatement(Mockito.contains(TEST_TABLE)))
               .thenReturn(preparedStatement);
        Mockito.when(preparedStatement.executeQuery())
               .thenReturn(resultSet);
        Mockito.when(resultSet.getMetaData())
               .thenReturn(resultSetMetaData);
        Mockito.when(resultSetMetaData.getColumnCount())
               .thenReturn(2);
        Mockito.when(resultSetMetaData.getColumnName(1))
               .thenReturn("id");
        Mockito.when(resultSetMetaData.getColumnName(2))
               .thenReturn("name");
        Mockito.when(resultSetMetaData.getColumnTypeName(1))
               .thenReturn("BIGINT");
        Mockito.when(resultSetMetaData.getColumnTypeName(2))
               .thenReturn("VARCHAR");
        Mockito.when(resultSet.next())
               .thenReturn(true, true, false);
        Mockito.when(resultSet.getObject("id"))
               .thenReturn(1L, 2L);
        Mockito.when(resultSet.getObject("name"))
               .thenReturn("alice", "bob");
        Mockito.when(sqlQueryExecutor.executeWithAutoCommit(Mockito.<SqlQuery<DatabaseTableData>> any()))
               .thenAnswer(invocation -> invocation.<SqlQuery<DatabaseTableData>> getArgument(0)
                                                   .execute(connection));

        DatabaseTableData tableData = databaseQueryClient.extractTableData(TEST_TABLE);

        Assertions.assertEquals(TEST_TABLE, tableData.getTableName());
        List<DatabaseTableColumnMetadata> columns = tableData.getTableColumnsMetadata();
        Assertions.assertEquals(2, columns.size());
        Assertions.assertEquals("id", columns.get(0)
                                             .getColumnName());
        Assertions.assertEquals("BIGINT", columns.get(0)
                                                 .getColumnType());
        Assertions.assertEquals("name", columns.get(1)
                                               .getColumnName());
        Assertions.assertEquals("VARCHAR", columns.get(1)
                                                  .getColumnType());

        List<DatabaseTableRowData> rows = tableData.getTableRowsData();
        Assertions.assertEquals(2, rows.size());
        Assertions.assertEquals(1L, rows.get(0)
                                        .getValues()
                                        .get("id"));
        Assertions.assertEquals("alice", rows.get(0)
                                             .getValues()
                                             .get("name"));
        Assertions.assertEquals(2L, rows.get(1)
                                        .getValues()
                                        .get("id"));
        Assertions.assertEquals("bob", rows.get(1)
                                           .getValues()
                                           .get("name"));
    }

    @Test
    void testExtractTableDataReturnsEmptyRowsWhenResultSetEmpty() throws SQLException {
        Mockito.when(connection.prepareStatement(Mockito.anyString()))
               .thenReturn(preparedStatement);
        Mockito.when(preparedStatement.executeQuery())
               .thenReturn(resultSet);
        Mockito.when(resultSet.getMetaData())
               .thenReturn(resultSetMetaData);
        Mockito.when(resultSetMetaData.getColumnCount())
               .thenReturn(1);
        Mockito.when(resultSetMetaData.getColumnName(1))
               .thenReturn("id");
        Mockito.when(resultSetMetaData.getColumnTypeName(1))
               .thenReturn("BIGINT");
        Mockito.when(resultSet.next())
               .thenReturn(false);
        Mockito.when(sqlQueryExecutor.executeWithAutoCommit(Mockito.<SqlQuery<DatabaseTableData>> any()))
               .thenAnswer(invocation -> invocation.<SqlQuery<DatabaseTableData>> getArgument(0)
                                                   .execute(connection));

        DatabaseTableData tableData = databaseQueryClient.extractTableData(TEST_TABLE);

        Assertions.assertEquals(1, tableData.getTableColumnsMetadata()
                                            .size());
        Assertions.assertTrue(tableData.getTableRowsData()
                                       .isEmpty());
    }

    @Test
    void testWriteDataToDataSourcePopulatesAndExecutesInsertForEveryRow() throws SQLException {
        DatabaseTableColumnMetadata idColumn = ImmutableDatabaseTableColumnMetadata.builder()
                                                                                   .columnName("id")
                                                                                   .columnType("int8")
                                                                                   .build();
        DatabaseTableColumnMetadata nameColumn = ImmutableDatabaseTableColumnMetadata.builder()
                                                                                     .columnName("name")
                                                                                     .columnType("varchar")
                                                                                     .build();
        DatabaseTableRowData rowOne = ImmutableDatabaseTableRowData.builder()
                                                                   .putValue("id", 1L)
                                                                   .putValue("name", "alice")
                                                                   .build();
        DatabaseTableRowData rowTwo = ImmutableDatabaseTableRowData.builder()
                                                                   .putValue("id", 2L)
                                                                   .putValue("name", "bob")
                                                                   .build();
        DatabaseTableData sourceTableData = ImmutableDatabaseTableData.builder()
                                                                      .tableName(TEST_TABLE)
                                                                      .addTableColumnsMetadata(idColumn, nameColumn)
                                                                      .addTableRowsData(rowOne, rowTwo)
                                                                      .build();
        String insertQuery = "INSERT INTO test_table (id, name) VALUES (?, ?)";

        Mockito.when(connection.prepareStatement(insertQuery))
               .thenReturn(preparedStatement);
        Mockito.when(sqlQueryExecutor.execute(Mockito.<SqlQuery<Object>> any()))
               .thenAnswer(invocation -> invocation.<SqlQuery<Object>> getArgument(0)
                                                   .execute(connection));

        databaseQueryClient.writeDataToDataSource(insertQuery, sourceTableData);

        Mockito.verify(connection, Mockito.times(2))
               .prepareStatement(insertQuery);
        Mockito.verify(preparedStatement, Mockito.times(2))
               .executeUpdate();
    }

    @Test
    void testWriteDataToDataSourceWithEmptyRowsDoesNotCallPrepareStatement() throws SQLException {
        DatabaseTableData emptyTableData = ImmutableDatabaseTableData.builder()
                                                                     .tableName(TEST_TABLE)
                                                                     .build();
        Mockito.when(sqlQueryExecutor.execute(Mockito.<SqlQuery<Object>> any()))
               .thenAnswer(invocation -> invocation.<SqlQuery<Object>> getArgument(0)
                                                   .execute(connection));

        databaseQueryClient.writeDataToDataSource("INSERT INTO test_table VALUES ()", emptyTableData);

        Mockito.verify(connection, Mockito.never())
               .prepareStatement(Mockito.anyString());
    }

}
package com.sap.cloud.lm.sl.cf.database.migration.executor;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.database.migration.client.DatabaseQueryClient;

class DatabaseSequenceMigrationExecutorTest {
    private static final String TEST_SEQUENCE_NAME = "test";
    private static final long ZERO_SEQUENCE_VALUE = 0L;
    private static final long NEGATIVE_SEQUENCE_VALUE = -1L;
    private static final long POSITIVE_SEQUENCE_VALUE = 1L;

    @Mock
    private DataSource mockSourceDataSource;
    @Mock
    private DataSource mockTargetDataSource;
    @Mock
    private DatabaseQueryClient mockSourceDatabaseQueryClient;
    @Mock
    private DatabaseQueryClient mockTargetDatabaseQueryClient;

    private ImmutableDatabaseSequenceMigrationExecutor databaseSequenceMigrationExecutor;

    @BeforeEach
    public void initialiseDatabaseTableMigrationExecutorWithMocks() {
        MockitoAnnotations.initMocks(this);
        databaseSequenceMigrationExecutor = ImmutableDatabaseSequenceMigrationExecutor.builder()
                                                                                      .sourceDataSource(mockSourceDataSource)
                                                                                      .targetDataSource(mockTargetDataSource)
                                                                                      .sourceDatabaseQueryClient(mockSourceDatabaseQueryClient)
                                                                                      .targetDatabaseQueryClient(mockTargetDatabaseQueryClient)
                                                                                      .build();
    }

    @Test
    public void testExecuteMigrationInternalWhenLastSequenceValueIsZero() throws SQLException {
        Mockito.when(mockSourceDatabaseQueryClient.getLastSequenceValue(TEST_SEQUENCE_NAME))
               .thenReturn(ZERO_SEQUENCE_VALUE);

        Assertions.assertThrows(IllegalStateException.class, () -> {
            databaseSequenceMigrationExecutor.executeMigrationInternal(TEST_SEQUENCE_NAME);
        });
    }

    @Test
    public void testExecuteMigrationInternalWhenLastSequenceValueIsNegative() throws SQLException {
        Mockito.when(mockSourceDatabaseQueryClient.getLastSequenceValue(TEST_SEQUENCE_NAME))
               .thenReturn(NEGATIVE_SEQUENCE_VALUE);

        Assertions.assertThrows(IllegalStateException.class, () -> {
            databaseSequenceMigrationExecutor.executeMigrationInternal(TEST_SEQUENCE_NAME);
        });
    }

    @Test
    public void testExecuteMigrationInternalWhenLastSequenceValueIsPositive() throws SQLException {
        Mockito.when(mockSourceDatabaseQueryClient.getLastSequenceValue(TEST_SEQUENCE_NAME))
               .thenReturn(POSITIVE_SEQUENCE_VALUE);

        databaseSequenceMigrationExecutor.executeMigrationInternal(TEST_SEQUENCE_NAME);

        Mockito.verify(mockTargetDatabaseQueryClient)
               .updateSequenceInDatabase(TEST_SEQUENCE_NAME, POSITIVE_SEQUENCE_VALUE);
    }

    @Test
    public void testExecuteMigrationInternalWhenSequenceNameIsNull() throws SQLException {
        Mockito.when(mockSourceDatabaseQueryClient.getLastSequenceValue(null))
               .thenReturn(POSITIVE_SEQUENCE_VALUE);

        databaseSequenceMigrationExecutor.executeMigrationInternal(null);

        Mockito.verify(mockTargetDatabaseQueryClient)
               .updateSequenceInDatabase(null, POSITIVE_SEQUENCE_VALUE);
    }

    @Test
    public void testExecuteMigrationInternalWhenSequenceNameIsEmpty() throws SQLException {
        Mockito.when(mockSourceDatabaseQueryClient.getLastSequenceValue(""))
               .thenReturn(POSITIVE_SEQUENCE_VALUE);

        databaseSequenceMigrationExecutor.executeMigrationInternal("");

        Mockito.verify(mockTargetDatabaseQueryClient)
               .updateSequenceInDatabase("", POSITIVE_SEQUENCE_VALUE);
    }

}

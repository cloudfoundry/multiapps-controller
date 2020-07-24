package org.cloudfoundry.multiapps.controller.database.migration.executor;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.cloudfoundry.multiapps.controller.database.migration.client.DatabaseQueryClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class DatabaseSequenceMigrationExecutorTest {

    private static final String TEST_SEQUENCE_NAME = "test";

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
    void initialiseDatabaseTableMigrationExecutorWithMocks() {
        MockitoAnnotations.initMocks(this);
        databaseSequenceMigrationExecutor = ImmutableDatabaseSequenceMigrationExecutor.builder()
                                                                                      .sourceDataSource(mockSourceDataSource)
                                                                                      .targetDataSource(mockTargetDataSource)
                                                                                      .sourceDatabaseQueryClient(mockSourceDatabaseQueryClient)
                                                                                      .targetDatabaseQueryClient(mockTargetDatabaseQueryClient)
                                                                                      .build();
    }

    @Test
    void testExecuteMigrationInternalWhenLastSequenceValueIsZero() throws SQLException {
        Mockito.when(mockSourceDatabaseQueryClient.getLastSequenceValue(TEST_SEQUENCE_NAME))
               .thenReturn(0L);

        Assertions.assertThrows(IllegalStateException.class, () -> databaseSequenceMigrationExecutor.executeMigration(TEST_SEQUENCE_NAME));
    }

    @Test
    void testExecuteMigrationInternalWhenLastSequenceValueIsNegative() throws SQLException {
        Mockito.when(mockSourceDatabaseQueryClient.getLastSequenceValue(TEST_SEQUENCE_NAME))
               .thenReturn(-1L);

        Assertions.assertThrows(IllegalStateException.class, () -> databaseSequenceMigrationExecutor.executeMigration(TEST_SEQUENCE_NAME));
    }

    @Test
    void testExecuteMigrationInternalWhenLastSequenceValueIsPositive() throws SQLException {
        Mockito.when(mockSourceDatabaseQueryClient.getLastSequenceValue(TEST_SEQUENCE_NAME))
               .thenReturn(1L);

        databaseSequenceMigrationExecutor.executeMigration(TEST_SEQUENCE_NAME);

        Mockito.verify(mockTargetDatabaseQueryClient)
               .updateSequence(TEST_SEQUENCE_NAME, 1L);
    }

}

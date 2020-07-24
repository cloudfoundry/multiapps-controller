package com.sap.cloud.lm.sl.cf.database.migration.executor;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.database.migration.client.DatabaseQueryClient;
import com.sap.cloud.lm.sl.cf.database.migration.generator.DatabaseTableInsertQueryGenerator;

class DatabaseTableMigrationExecutorTest {

    private static final String TEST_DATABASE_TABLE = "test";

    @Mock
    private DataSource mockSourceDataSource;
    @Mock
    private DataSource mockTargetDataSource;
    @Mock
    private DatabaseQueryClient mockSourceDatabaseQueryClient;
    @Mock
    private DatabaseQueryClient mockTargetDatabaseQueryClient;
    @Mock
    private DatabaseTableInsertQueryGenerator mockDatabaseTableInsertQueryGenerator;

    private ImmutableDatabaseTableMigrationExecutor databaseTableMigrationExecutor;

    @BeforeEach
    void initialiseDatabaseTableMigrationExecutorAndConfigureMocks() throws SQLException {
        MockitoAnnotations.initMocks(this);
        databaseTableMigrationExecutor = ImmutableDatabaseTableMigrationExecutor.builder()
                                                                                .sourceDataSource(mockSourceDataSource)
                                                                                .targetDataSource(mockTargetDataSource)
                                                                                .sourceDatabaseQueryClient(mockSourceDatabaseQueryClient)
                                                                                .targetDatabaseQueryClient(mockTargetDatabaseQueryClient)
                                                                                .databaseTableInsertQueryGenerator(mockDatabaseTableInsertQueryGenerator)
                                                                                .build();
        Mockito.doReturn("")
               .when(mockDatabaseTableInsertQueryGenerator)
               .generate(null);
    }

    @Test
    void testExecuteMigrationInternalWithTestDatabaseTableString() throws SQLException {
        databaseTableMigrationExecutor.executeMigration(TEST_DATABASE_TABLE);

        Mockito.verify(mockSourceDatabaseQueryClient)
               .extractTableData(TEST_DATABASE_TABLE);
        Mockito.verify(mockTargetDatabaseQueryClient)
               .writeDataToDataSource("", null);
    }

}

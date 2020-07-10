package com.sap.cloud.lm.sl.cf.database.migration.executor;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.database.migration.client.DatabaseQueryClient;
import com.sap.cloud.lm.sl.cf.database.migration.generator.DatabaseTableInsertQueryGenerator;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.DatabaseTableMetadata;

class DatabaseTableMigrationExecutorTest {
    private static final String TEST_DATABASE_TABLE = "test";
    private static final DatabaseTableMetadata DATABASE_TABLE_METADATA = null;
    private static final ResultSet RESULT_SET = null;

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
    public void initialiseDatabaseTableMigrationExecutorAndConfigureMocks() throws SQLException {
        MockitoAnnotations.initMocks(this);
        databaseTableMigrationExecutor = ImmutableDatabaseTableMigrationExecutor.builder()
                                                                                .sourceDataSource(mockSourceDataSource)
                                                                                .targetDataSource(mockTargetDataSource)
                                                                                .sourceDatabaseQueryClient(mockSourceDatabaseQueryClient)
                                                                                .targetDatabaseQueryClient(mockTargetDatabaseQueryClient)
                                                                                .databaseTableInsertQueryGenerator(mockDatabaseTableInsertQueryGenerator)
                                                                                .build();
        Mockito.doReturn(DATABASE_TABLE_METADATA)
               .when(mockSourceDatabaseQueryClient)
               .extractTableMetadataFromDatabase(Mockito.anyString());
        Mockito.doReturn(RESULT_SET)
               .when(mockSourceDatabaseQueryClient)
               .getDataFromDataSource(Mockito.anyString());
        Mockito.doReturn("")
               .when(mockDatabaseTableInsertQueryGenerator)
               .generate(DATABASE_TABLE_METADATA);
    }

    @Test
    public void testExecuteMigrationInternalWithTestDatabaseTableString() throws SQLException {
        databaseTableMigrationExecutor.executeMigrationInternal(TEST_DATABASE_TABLE);

        Mockito.verify(mockSourceDatabaseQueryClient)
               .extractTableMetadataFromDatabase(TEST_DATABASE_TABLE);
        Mockito.verify(mockSourceDatabaseQueryClient)
               .getDataFromDataSource(TEST_DATABASE_TABLE);
        Mockito.verify(mockTargetDatabaseQueryClient)
               .writeDataToDataSource(RESULT_SET, "", DATABASE_TABLE_METADATA);
    }

    @Test
    public void testExecuteMigrationInternalWithNullDatabaseTableString() throws SQLException {
        databaseTableMigrationExecutor.executeMigrationInternal(null);

        Mockito.verify(mockSourceDatabaseQueryClient)
               .extractTableMetadataFromDatabase(null);
        Mockito.verify(mockSourceDatabaseQueryClient)
               .getDataFromDataSource(null);
        Mockito.verify(mockTargetDatabaseQueryClient)
               .writeDataToDataSource(null, "", null);
    }

    @Test
    public void testExecuteMigrationInternalWithEmptyDatabaseTableString() throws SQLException {
        databaseTableMigrationExecutor.executeMigrationInternal("");

        Mockito.verify(mockSourceDatabaseQueryClient)
               .extractTableMetadataFromDatabase("");
        Mockito.verify(mockSourceDatabaseQueryClient)
               .getDataFromDataSource("");
        Mockito.verify(mockTargetDatabaseQueryClient)
               .writeDataToDataSource(null, "", null);
    }

}

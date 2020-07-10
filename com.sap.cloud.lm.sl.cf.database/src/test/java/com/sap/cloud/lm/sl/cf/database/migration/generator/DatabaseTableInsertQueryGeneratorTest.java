package com.sap.cloud.lm.sl.cf.database.migration.generator;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.database.migration.metadata.DatabaseTableColumnMetadata;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.DatabaseTableMetadata;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.ImmutableDatabaseTableColumnMetadata;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.ImmutableDatabaseTableMetadata;

class DatabaseTableInsertQueryGeneratorTest {
    private static final String TEST_TABLE_NAME = "testTableName";
    private static final String TEST_COLUMN_NAME = "testColumnName";
    private static final String TEST_COLUMN_TYPE = "testColumnType";

    private DatabaseTableInsertQueryGenerator databaseTableInsertQueryGenerator;

    @Test
    public void testGenerateWithTestDatabaseMetadataWhenSingleColumn() {
        databaseTableInsertQueryGenerator = new DatabaseTableInsertQueryGenerator();
        DatabaseTableColumnMetadata databaseTableColumnMetadata = buildTestDatabaseTableColumnMetadata(TEST_COLUMN_NAME, TEST_COLUMN_TYPE);
        DatabaseTableMetadata databaseTableMetadata = buildTestDatabaseTableMetadata(Arrays.asList(databaseTableColumnMetadata),
                                                                                     TEST_TABLE_NAME);

        String expectedQuery = "INSERT INTO testTableName(testColumnName) VALUES (?)";
        String resultQuery = databaseTableInsertQueryGenerator.generate(databaseTableMetadata);

        Assertions.assertEquals(expectedQuery, resultQuery);
    }

    @Test
    public void testGenerateWithTestDatabaseMetadataWhenMultipleColumns() {
        databaseTableInsertQueryGenerator = new DatabaseTableInsertQueryGenerator();
        List<DatabaseTableColumnMetadata> multipleDatabaseTableColumnMetadata = Arrays.asList(buildTestDatabaseTableColumnMetadata(TEST_COLUMN_NAME,
                                                                                                                                   TEST_COLUMN_TYPE),
                                                                                              buildTestDatabaseTableColumnMetadata(TEST_COLUMN_NAME,
                                                                                                                                   TEST_COLUMN_TYPE));
        DatabaseTableMetadata databaseTableMetadata = buildTestDatabaseTableMetadata(multipleDatabaseTableColumnMetadata, TEST_TABLE_NAME);

        String expectedQuery = "INSERT INTO testTableName(testColumnName, testColumnName) VALUES (?, ?)";
        String resultQuery = databaseTableInsertQueryGenerator.generate(databaseTableMetadata);

        Assertions.assertEquals(expectedQuery, resultQuery);
    }

    private ImmutableDatabaseTableColumnMetadata buildTestDatabaseTableColumnMetadata(String columnName, String columnType) {
        return ImmutableDatabaseTableColumnMetadata.builder()
                                                   .columnName(columnName)
                                                   .columnType(columnType)
                                                   .build();
    }

    private ImmutableDatabaseTableMetadata
            buildTestDatabaseTableMetadata(List<DatabaseTableColumnMetadata> multipleDatabaseTableColumnMetadata, String tableName) {
        return ImmutableDatabaseTableMetadata.builder()
                                             .tableName(tableName)
                                             .addAllTableColumnsMetadata(multipleDatabaseTableColumnMetadata)
                                             .build();
    }

}

package com.sap.cloud.lm.sl.cf.database.migration.generator;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.cloud.lm.sl.cf.database.migration.metadata.DatabaseTableColumnMetadata;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.DatabaseTableData;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.ImmutableDatabaseTableColumnMetadata;
import com.sap.cloud.lm.sl.cf.database.migration.metadata.ImmutableDatabaseTableData;

class DatabaseTableInsertQueryGeneratorTest {

    private static final String TEST_TABLE_NAME = "testTableName";
    private static final String TEST_COLUMN_NAME = "testColumnName";
    private static final String TEST_COLUMN_NAME_2 = "testColumnName2";
    private static final String TEST_COLUMN_TYPE = "testColumnType";

    private DatabaseTableInsertQueryGenerator databaseTableInsertQueryGenerator = new DatabaseTableInsertQueryGenerator();

    @Test
    public void testGenerateWithTestDatabaseMetadataWhenSingleColumn() {
        DatabaseTableColumnMetadata databaseTableColumnMetadata = buildDatabaseTableColumnMetadata(TEST_COLUMN_NAME, TEST_COLUMN_TYPE);
        DatabaseTableData databaseTableMetadata = buildDatabaseTableData(Arrays.asList(databaseTableColumnMetadata), TEST_TABLE_NAME);

        String expectedQuery = "INSERT INTO testTableName(testColumnName) VALUES (?)";
        String resultQuery = databaseTableInsertQueryGenerator.generate(databaseTableMetadata);

        Assertions.assertEquals(expectedQuery, resultQuery);
    }

    @Test
    public void testGenerateWithTestDatabaseMetadataWhenMultipleColumns() {
        List<DatabaseTableColumnMetadata> multipleDatabaseTableColumnMetadata = Arrays.asList(buildDatabaseTableColumnMetadata(TEST_COLUMN_NAME,
                                                                                                                               TEST_COLUMN_TYPE),
                                                                                              buildDatabaseTableColumnMetadata(TEST_COLUMN_NAME_2,
                                                                                                                               TEST_COLUMN_TYPE));
        DatabaseTableData databaseTableMetadata = buildDatabaseTableData(multipleDatabaseTableColumnMetadata, TEST_TABLE_NAME);

        String resultQuery = databaseTableInsertQueryGenerator.generate(databaseTableMetadata);

        String expectedQuery = "INSERT INTO testTableName(testColumnName, testColumnName2) VALUES (?, ?)";
        Assertions.assertEquals(expectedQuery, resultQuery);
    }

    private ImmutableDatabaseTableColumnMetadata buildDatabaseTableColumnMetadata(String columnName, String columnType) {
        return ImmutableDatabaseTableColumnMetadata.builder()
                                                   .columnName(columnName)
                                                   .columnType(columnType)
                                                   .build();
    }

    private ImmutableDatabaseTableData buildDatabaseTableData(List<DatabaseTableColumnMetadata> multipleDatabaseTableColumnMetadata,
                                                              String tableName) {
        return ImmutableDatabaseTableData.builder()
                                         .tableName(tableName)
                                         .addAllTableColumnsMetadata(multipleDatabaseTableColumnMetadata)
                                         .build();
    }

}

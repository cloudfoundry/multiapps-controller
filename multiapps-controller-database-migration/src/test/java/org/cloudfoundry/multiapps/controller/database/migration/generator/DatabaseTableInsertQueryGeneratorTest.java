package org.cloudfoundry.multiapps.controller.database.migration.generator;

import java.util.List;

import org.cloudfoundry.multiapps.controller.database.migration.metadata.DatabaseTableColumnMetadata;
import org.cloudfoundry.multiapps.controller.database.migration.metadata.DatabaseTableData;
import org.cloudfoundry.multiapps.controller.database.migration.metadata.ImmutableDatabaseTableColumnMetadata;
import org.cloudfoundry.multiapps.controller.database.migration.metadata.ImmutableDatabaseTableData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DatabaseTableInsertQueryGeneratorTest {

    private static final String TEST_TABLE_NAME = "testTableName";
    private static final String TEST_COLUMN_NAME = "testColumnName";
    private static final String TEST_COLUMN_NAME_2 = "testColumnName2";
    private static final String TEST_COLUMN_TYPE = "testColumnType";

    private final DatabaseTableInsertQueryGenerator databaseTableInsertQueryGenerator = new DatabaseTableInsertQueryGenerator();

    @Test
    void testGenerateWithTestDatabaseMetadataWhenSingleColumn() {
        DatabaseTableColumnMetadata databaseTableColumnMetadata = buildDatabaseTableColumnMetadata(TEST_COLUMN_NAME, TEST_COLUMN_TYPE);
        DatabaseTableData databaseTableMetadata = buildDatabaseTableData(List.of(databaseTableColumnMetadata), TEST_TABLE_NAME);

        String expectedQuery = "INSERT INTO testTableName(testColumnName) VALUES (?)";
        String resultQuery = databaseTableInsertQueryGenerator.generate(databaseTableMetadata);

        Assertions.assertEquals(expectedQuery, resultQuery);
    }

    @Test
    void testGenerateWithTestDatabaseMetadataWhenMultipleColumns() {
        List<DatabaseTableColumnMetadata> multipleDatabaseTableColumnMetadata = List.of(buildDatabaseTableColumnMetadata(TEST_COLUMN_NAME,
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

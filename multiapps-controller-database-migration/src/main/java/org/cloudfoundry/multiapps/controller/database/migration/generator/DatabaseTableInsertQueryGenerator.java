package org.cloudfoundry.multiapps.controller.database.migration.generator;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.cloudfoundry.multiapps.controller.database.migration.metadata.DatabaseTableColumnMetadata;
import org.cloudfoundry.multiapps.controller.database.migration.metadata.DatabaseTableData;

public class DatabaseTableInsertQueryGenerator {

    private static final String CLOSING_BRACKET = ")";
    private static final String OPEN_BRACKET = "(";
    private static final String DEFAULT_STATEMENT_VALUES_SEPARATOR = ", ";
    private static final String DEFAULT_STATEMENT_PARAMETER = "?";

    public String generate(DatabaseTableData tableMetadata) {
        StringBuilder result = new StringBuilder();
        return result.append("INSERT INTO ")
                     .append(tableMetadata.getTableName())
                     .append(OPEN_BRACKET)
                     .append(generateInsertStatementTableColums(tableMetadata.getTableColumnsMetadata()))
                     .append(CLOSING_BRACKET)
                     .append(" VALUES ")
                     .append(OPEN_BRACKET)
                     .append(generateInsertStatementParameters(tableMetadata.getTableColumnsMetadata()
                                                                            .size()))
                     .append(CLOSING_BRACKET)
                     .toString();
    }

    private String generateInsertStatementTableColums(List<DatabaseTableColumnMetadata> tableColumnsMetadata) {
        return tableColumnsMetadata.stream()
                                   .map(DatabaseTableColumnMetadata::getColumnName)
                                   .collect(Collectors.joining(DEFAULT_STATEMENT_VALUES_SEPARATOR));
    }

    private String generateInsertStatementParameters(int numberOfParameters) {
        return IntStream.rangeClosed(1, numberOfParameters)
                        .mapToObj(integer -> DEFAULT_STATEMENT_PARAMETER)
                        .collect(Collectors.joining(DEFAULT_STATEMENT_VALUES_SEPARATOR));
    }

}

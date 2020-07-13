package com.sap.cloud.lm.sl.cf.database.migration.metadata;

import java.util.List;

import org.immutables.value.Value.Immutable;

@Immutable
public interface DatabaseTableData {

    String getTableName();

    List<DatabaseTableColumnMetadata> getTableColumnsMetadata();

    List<DatabaseTableRowData> getTableRowsData();

}

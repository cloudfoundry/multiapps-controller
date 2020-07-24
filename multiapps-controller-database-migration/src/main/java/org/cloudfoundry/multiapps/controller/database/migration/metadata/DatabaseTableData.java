package org.cloudfoundry.multiapps.controller.database.migration.metadata;

import java.util.List;

import org.immutables.value.Value.Immutable;

@Immutable
public interface DatabaseTableData {

    String getTableName();

    List<DatabaseTableColumnMetadata> getTableColumnsMetadata();

    List<DatabaseTableRowData> getTableRowsData();

}

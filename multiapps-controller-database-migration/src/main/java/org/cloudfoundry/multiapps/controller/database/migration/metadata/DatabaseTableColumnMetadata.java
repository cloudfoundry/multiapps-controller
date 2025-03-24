package org.cloudfoundry.multiapps.controller.database.migration.metadata;

import org.immutables.value.Value.Immutable;

@Immutable
public interface DatabaseTableColumnMetadata {

    String getColumnName();

    String getColumnType();

}

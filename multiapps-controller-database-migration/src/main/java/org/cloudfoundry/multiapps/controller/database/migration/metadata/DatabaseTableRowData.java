package org.cloudfoundry.multiapps.controller.database.migration.metadata;

import java.util.Map;

import org.cloudfoundry.multiapps.common.MultiappsImmutablesStyle;
import org.cloudfoundry.multiapps.common.SkipNulls;
import org.immutables.value.Value;

@MultiappsImmutablesStyle
@Value.Immutable
public interface DatabaseTableRowData {

    @SkipNulls
    Map<String, Object> getValues();

}

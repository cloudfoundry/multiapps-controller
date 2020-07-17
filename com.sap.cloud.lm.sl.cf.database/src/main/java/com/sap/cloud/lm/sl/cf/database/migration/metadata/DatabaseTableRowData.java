package com.sap.cloud.lm.sl.cf.database.migration.metadata;

import java.util.Map;

import org.immutables.value.Value;

import com.sap.cloud.lm.sl.common.MultiappsImmutablesStyle;
import com.sap.cloud.lm.sl.common.SkipNulls;

@MultiappsImmutablesStyle
@Value.Immutable
public interface DatabaseTableRowData {

    @SkipNulls
    Map<String, Object> getValues();

}

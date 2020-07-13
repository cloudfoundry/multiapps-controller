package com.sap.cloud.lm.sl.cf.database.migration.metadata;

import java.util.Map;

import org.immutables.value.Value;

import com.sap.cloud.lm.sl.common.SkipNulls;

@Value.Style(jdkOnly = true)
@Value.Immutable
public interface DatabaseTableRowData {

    @SkipNulls
    Map<String, Object> getValues();

}

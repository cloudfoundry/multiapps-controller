package com.sap.cloud.lm.sl.cf.core.changes;

import com.sap.cloud.lm.sl.cf.persistence.changes.AbstractIndexSQLChange;

public class IndexSpaceIdOfOperationChange extends AbstractIndexSQLChange {

    @Override
    protected String getQuery() {
        return "CREATE INDEX CONCURRENTLY IDX_OPERATION_SPACE_ID ON OPERATION(SPACE_ID)";
    }

    @Override
    protected String getIndexName() {
        return "IDX_OPERATION_SPACE_ID";
    }

}

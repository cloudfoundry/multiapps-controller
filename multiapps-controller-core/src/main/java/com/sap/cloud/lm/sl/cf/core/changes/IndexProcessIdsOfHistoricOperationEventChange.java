package com.sap.cloud.lm.sl.cf.core.changes;

import com.sap.cloud.lm.sl.cf.persistence.changes.AbstractIndexSQLChange;

public class IndexProcessIdsOfHistoricOperationEventChange extends AbstractIndexSQLChange {

    @Override
    protected String getQuery() {
        return "CREATE INDEX CONCURRENTLY IDX_HISTORIC_OPERATION_EVENT_PROCESS_ID ON HISTORIC_OPERATION_EVENT(PROCESS_ID)";
    }

    @Override
    protected String getIndexName() {
        return "IDX_HISTORIC_OPERATION_EVENT_PROCESS_ID";
    }

}

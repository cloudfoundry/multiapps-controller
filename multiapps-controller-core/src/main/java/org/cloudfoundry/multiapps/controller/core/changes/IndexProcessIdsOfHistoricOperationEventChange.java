package org.cloudfoundry.multiapps.controller.core.changes;

import org.cloudfoundry.multiapps.controller.persistence.changes.AbstractIndexSQLChange;

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

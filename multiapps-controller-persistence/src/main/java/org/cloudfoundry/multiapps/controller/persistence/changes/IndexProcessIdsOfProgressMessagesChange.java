package org.cloudfoundry.multiapps.controller.persistence.changes;

public class IndexProcessIdsOfProgressMessagesChange extends AbstractIndexSQLChange {

    @Override
    protected String getQuery() {
        return "CREATE INDEX CONCURRENTLY IDX_PROGRESS_MESSAGE_PROCESS_ID ON PROGRESS_MESSAGE(PROCESS_ID)";
    }

    @Override
    protected String getIndexName() {
        return "IDX_PROGRESS_MESSAGE_PROCESS_ID";
    }

}

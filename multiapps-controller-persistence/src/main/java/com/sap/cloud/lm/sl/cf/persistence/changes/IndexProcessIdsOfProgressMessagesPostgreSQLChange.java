package com.sap.cloud.lm.sl.cf.persistence.changes;

public class IndexProcessIdsOfProgressMessagesPostgreSQLChange extends AbstractIndexSQLChange {

    @Override
    protected String getQuery() {
        return "CREATE INDEX CONCURRENTLY IDX_PROGRESS_MESSAGE_PROCESS_ID ON PROGRESS_MESSAGE(PROCESS_ID)";
    }

    @Override
    protected String getIndexName() {
        return "IDX_PROGRESS_MESSAGE_PROCESS_ID";
    }

}

package com.sap.cloud.lm.sl.cf.persistence.changes;

public class IndexSpaceOfLmSlPersistenceFileChange extends AbstractIndexSQLChange {

    @Override
    protected String getQuery() {
        return "CREATE INDEX CONCURRENTLY IF NOT EXISTS IDX_LM_SL_PERSISTENCE_SPACE ON LM_SL_PERSISTENCE_FILE(SPACE)";
    }

    @Override
    protected String getIndexName() {
        return "IDX_LM_SL_PERSISTENCE_SPACE";
    }

}

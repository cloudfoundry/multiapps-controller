package com.sap.cloud.lm.sl.cf.persistence.changes;

public class IndexSpaceOfLmSlPersistenceFilePostgreSQLChange extends AbstractIndexSQLChange {

    @Override
    protected String getQuery() {
        return "CREATE INDEX CONCURRENTLY IDX_LM_SL_PERSISTENCE_SPACE ON LM_SL_PERSISTENCE_FILE(SPACE)";
    }

    @Override
    protected String getIndexName() {
        return "IDX_LM_SL_PERSISTENCE_SPACE";
    }

}

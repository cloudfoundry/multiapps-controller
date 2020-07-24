package org.cloudfoundry.multiapps.controller.persistence.changes;

public class IndexSpaceOfLmSlPersistenceFileChange extends AbstractIndexSQLChange {

    @Override
    protected String getQuery() {
        return "CREATE INDEX CONCURRENTLY IDX_LM_SL_PERSISTENCE_SPACE ON LM_SL_PERSISTENCE_FILE(SPACE)";
    }

    @Override
    protected String getIndexName() {
        return "IDX_LM_SL_PERSISTENCE_SPACE";
    }

}

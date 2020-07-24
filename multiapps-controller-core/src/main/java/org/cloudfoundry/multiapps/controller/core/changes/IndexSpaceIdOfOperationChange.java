package org.cloudfoundry.multiapps.controller.core.changes;

import org.cloudfoundry.multiapps.controller.persistence.changes.AbstractIndexSQLChange;

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

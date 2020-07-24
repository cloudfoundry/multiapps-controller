package org.cloudfoundry.multiapps.controller.persistence;

import javax.sql.DataSource;

import org.cloudfoundry.multiapps.controller.persistence.dialects.DataSourceDialect;
import org.cloudfoundry.multiapps.controller.persistence.dialects.DefaultDataSourceDialect;

public class DataSourceWithDialect {

    private final DataSource dataSource;
    private final DataSourceDialect dataSourceDialect;

    public DataSourceWithDialect(DataSource dataSource) {
        this(dataSource, new DefaultDataSourceDialect());
    }

    public DataSourceWithDialect(DataSource dataSource, DataSourceDialect dataSourceDialect) {
        this.dataSource = dataSource;
        this.dataSourceDialect = dataSourceDialect;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public DataSourceDialect getDataSourceDialect() {
        return dataSourceDialect;
    }

}

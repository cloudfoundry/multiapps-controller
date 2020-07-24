package com.sap.cloud.lm.sl.cf.persistence;

import javax.sql.DataSource;

import com.sap.cloud.lm.sl.cf.persistence.dialects.DataSourceDialect;
import com.sap.cloud.lm.sl.cf.persistence.dialects.DefaultDataSourceDialect;

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

package org.cloudfoundry.multiapps.controller.database.migration.extractor;

import javax.sql.DataSource;

import io.pivotal.cfenv.jdbc.CfJdbcService;
import org.cloudfoundry.multiapps.controller.persistence.dto.DatabaseServiceKey;
import org.cloudfoundry.multiapps.controller.persistence.util.DataSourceFactory;
import org.cloudfoundry.multiapps.controller.persistence.util.EnvironmentServicesFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataSourceEnvironmentExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceEnvironmentExtractor.class);

    public DataSource extractDataSource(String serviceName) {
        LOGGER.info("Creating a data source for service {}...", serviceName);
        CfJdbcService service = findService(serviceName);
        return createDataSource(service);
    }

    public DataSource extractDataSource(DatabaseServiceKey databaseServiceKey) {
        return createDataSource(databaseServiceKey);
    }

    private CfJdbcService findService(String serviceName) {
        return new EnvironmentServicesFinder().findJdbcService(serviceName);
    }

    private DataSource createDataSource(CfJdbcService service) {
        return new DataSourceFactory().createDataSource(service);
    }

    private DataSource createDataSource(DatabaseServiceKey databaseServiceKey) {
        return new DataSourceFactory().createDataSource(databaseServiceKey);
    }
}

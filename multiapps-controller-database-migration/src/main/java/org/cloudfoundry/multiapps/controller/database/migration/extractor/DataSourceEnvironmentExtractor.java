package org.cloudfoundry.multiapps.controller.database.migration.extractor;

import javax.sql.DataSource;

import org.cloudfoundry.multiapps.controller.persistence.util.DataSourceFactory;
import org.cloudfoundry.multiapps.controller.persistence.util.EnvironmentServicesFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pivotal.cfenv.jdbc.CfJdbcService;

public class DataSourceEnvironmentExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceEnvironmentExtractor.class);

    public DataSource extractDataSource(String serviceName) {
        LOGGER.info("Creating a data source for service {}...", serviceName);
        CfJdbcService service = findService(serviceName);
        return createDataSource(service);
    }

    private CfJdbcService findService(String serviceName) {
        return new EnvironmentServicesFinder().findJdbcService(serviceName);
    }

    private DataSource createDataSource(CfJdbcService service) {
        return new DataSourceFactory().createDataSource(service);
    }

}

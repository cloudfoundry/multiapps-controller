package com.sap.cloud.lm.sl.cf.database.migration.extractor;

import javax.sql.DataSource;

import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pivotal.cfenv.core.CfCredentials;
import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;

public class DataSourceEnvironmentExtractor {

    private final static Logger LOGGER = LoggerFactory.getLogger(DataSourceEnvironmentExtractor.class);

    public DataSource extractDataSource(String serviceName) {
        LOGGER.info("Extracting datasource for service {}...", serviceName  );
        CfCredentials databaseServiceCredentials = extractDatabaseServiceCredentials(serviceName);
        return extractDataSource(databaseServiceCredentials);
    }

    private CfCredentials extractDatabaseServiceCredentials(String serviceName) {
        CfEnv cfEnv = new CfEnv();
        CfService sourceService = cfEnv.findServiceByName(serviceName);
        return sourceService.getCredentials();
    }

    private PGSimpleDataSource extractDataSource(CfCredentials databaseServiceCredentials) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setServerName(databaseServiceCredentials.getHost());
        dataSource.setUser(databaseServiceCredentials.getUsername());
        dataSource.setPassword(databaseServiceCredentials.getPassword());
        dataSource.setDatabaseName(databaseServiceCredentials.getString("dbname"));
        dataSource.setPortNumber(getPort(databaseServiceCredentials));
        dataSource.setSsl(false);
        return dataSource;
    }

    private int getPort(CfCredentials databaseServiceCredentials) {
        return Integer.valueOf(databaseServiceCredentials.getPort());
    }

}

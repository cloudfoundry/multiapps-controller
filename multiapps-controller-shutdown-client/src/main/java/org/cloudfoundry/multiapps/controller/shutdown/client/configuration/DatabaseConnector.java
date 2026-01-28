package org.cloudfoundry.multiapps.controller.shutdown.client.configuration;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;
import org.cloudfoundry.multiapps.controller.database.migration.extractor.DataSourceEnvironmentExtractor;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;

public class DatabaseConnector {

    private static final String DEPLOY_SERVICE_DATABASE_NAME = "deploy-service-database";
    private static final String PERSIST_UNIT_NAME = "Default";
    private static final String PACKAGES_TO_SCAN = "org.cloudfoundry.multiapps";

    public EntityManagerFactory createEntityManagerFactory() {
        DataSourceEnvironmentExtractor environmentExtractor = new DataSourceEnvironmentExtractor();
        DataSource targetDataSource = environmentExtractor.extractDataSource(DEPLOY_SERVICE_DATABASE_NAME);

        return getLocalContainerEntityManagerFactoryBean(targetDataSource, eclipseLinkJpaVendorAdapter()).getObject();
    }

    private LocalContainerEntityManagerFactoryBean getLocalContainerEntityManagerFactoryBean(DataSource dataSource,
                                                                                             EclipseLinkJpaVendorAdapter eclipseLinkJpaVendorAdapter) {
        LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        localContainerEntityManagerFactoryBean.setPersistenceUnitName(PERSIST_UNIT_NAME);
        localContainerEntityManagerFactoryBean.setDataSource(dataSource);
        localContainerEntityManagerFactoryBean.setJpaVendorAdapter(eclipseLinkJpaVendorAdapter);
        localContainerEntityManagerFactoryBean.setPackagesToScan(PACKAGES_TO_SCAN);
        localContainerEntityManagerFactoryBean.afterPropertiesSet();
        return localContainerEntityManagerFactoryBean;
    }

    private EclipseLinkJpaVendorAdapter eclipseLinkJpaVendorAdapter() {
        EclipseLinkJpaVendorAdapter eclipseLinkJpaVendorAdapter = new EclipseLinkJpaVendorAdapter();
        eclipseLinkJpaVendorAdapter.setShowSql(false);
        eclipseLinkJpaVendorAdapter.setDatabase(Database.POSTGRESQL);
        return eclipseLinkJpaVendorAdapter;
    }

}

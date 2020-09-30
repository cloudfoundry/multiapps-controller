package org.cloudfoundry.multiapps.controller.web.configuration;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.DataSourceWithDialect;
import org.cloudfoundry.multiapps.controller.persistence.dialects.DataSourceDialect;
import org.cloudfoundry.multiapps.controller.persistence.dialects.DefaultDataSourceDialect;
import org.cloudfoundry.multiapps.controller.persistence.util.DataSourceFactory;
import org.cloudfoundry.multiapps.controller.persistence.util.EnvironmentServicesFinder;
import org.cloudfoundry.multiapps.controller.web.configuration.bean.factory.CloudDataSourceFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;

import liquibase.integration.spring.SpringLiquibase;

@Configuration
public class DatabaseConfiguration {

    private static final String DATA_SOURCE_SERVICE_NAME = "deploy-service-database";
    private static final String LIQUIBASE_CHANGELOG = "classpath:/org/cloudfoundry/multiapps/controller/persistence/db/changelog/db-changelog.xml";
    private static final String ENTITY_MANAGER_DEFAULT_PERSISTENCE_UNIT_NAME = "Default";

    @Inject
    @Bean
    public CloudDataSourceFactoryBean dataSource(DataSourceFactory dataSourceFactory, EnvironmentServicesFinder vcapServiceFinder,
                                                 ApplicationConfiguration configuration) {
        return new CloudDataSourceFactoryBean(DATA_SOURCE_SERVICE_NAME, dataSourceFactory, vcapServiceFinder, configuration);
    }

    @Bean
    public DefaultDataSourceDialect dataSourceDialect() {
        return new DefaultDataSourceDialect();
    }

    @Inject
    @Bean
    public DataSourceWithDialect dataSourceWithDialect(DataSource dataSource, DataSourceDialect dataSourceDialect) {
        return new DataSourceWithDialect(dataSource, dataSourceDialect);
    }

    @Inject
    @Bean
    public DataSourceTransactionManager transactionManager(DataSource dataSource, ApplicationConfiguration applicationConfiguration) {
        DataSourceTransactionManager dataSourceTransactionManager = new DataSourceTransactionManager(dataSource);
        dataSourceTransactionManager.setDefaultTimeout(applicationConfiguration.getDbTransactionTimeoutInSeconds());
        return dataSourceTransactionManager;
    }

    @Inject
    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean defaultEntityManagerFactory(DataSource dataSource,
                                                                              EclipseLinkJpaVendorAdapter eclipseLinkJpaVendorAdapter) {
        return getLocalContainerEntityManagerFactoryBean(dataSource, eclipseLinkJpaVendorAdapter,
                                                         ENTITY_MANAGER_DEFAULT_PERSISTENCE_UNIT_NAME);
    }

    protected LocalContainerEntityManagerFactoryBean
              getLocalContainerEntityManagerFactoryBean(DataSource dataSource, EclipseLinkJpaVendorAdapter eclipseLinkJpaVendorAdapter,
                                                        String persistenceUnitName) {
        LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        localContainerEntityManagerFactoryBean.setPersistenceUnitName(persistenceUnitName);
        localContainerEntityManagerFactoryBean.setDataSource(dataSource);
        localContainerEntityManagerFactoryBean.setJpaVendorAdapter(eclipseLinkJpaVendorAdapter);
        localContainerEntityManagerFactoryBean.setPackagesToScan("org.cloudfoundry.multiapps");
        return localContainerEntityManagerFactoryBean;
    }

    @Bean
    public EclipseLinkJpaVendorAdapter eclipseLinkJpaVendorAdapter() {
        EclipseLinkJpaVendorAdapter eclipseLinkJpaVendorAdapter = new EclipseLinkJpaVendorAdapter();
        eclipseLinkJpaVendorAdapter.setShowSql(false);
        eclipseLinkJpaVendorAdapter.setDatabase(Database.POSTGRESQL);
        return eclipseLinkJpaVendorAdapter;
    }

    @Inject
    @Bean
    public SpringLiquibase persistenceChangelog(DataSource dataSource) {
        return getLiquibaseTemplate(dataSource, LIQUIBASE_CHANGELOG);
    }

    private SpringLiquibase getLiquibaseTemplate(DataSource dataSource, String changeLog) {
        SpringLiquibase springLiquibase = new SpringLiquibase();
        springLiquibase.setDataSource(dataSource);
        springLiquibase.setChangeLog(changeLog);
        return springLiquibase;
    }

}

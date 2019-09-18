package com.sap.cloud.lm.sl.cf.web.configuration;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;

import com.sap.cloud.lm.sl.cf.core.changes.IndexSpaceIdOfOperationChange;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.persistence.DataSourceWithDialect;
import com.sap.cloud.lm.sl.cf.persistence.changes.IndexProcessIdsOfProgressMessagesChange;
import com.sap.cloud.lm.sl.cf.persistence.changes.IndexSpaceOfLmSlPersistenceFileChange;
import com.sap.cloud.lm.sl.cf.persistence.dialects.DataSourceDialect;
import com.sap.cloud.lm.sl.cf.persistence.dialects.DefaultDataSourceDialect;
import com.sap.cloud.lm.sl.cf.web.configuration.bean.factory.CloudDataSourceFactoryBean;

import liquibase.integration.spring.SpringLiquibase;

@Configuration
public class DatabaseConfiguration {

    private static final String DATA_SOURCE_SERVICE_NAME = "deploy-service-database";
    private static final String PERSISTENCE_CHANGE_LOG = "classpath:/com/sap/cloud/lm/sl/cf/persistence/db/changelog/db-changelog.xml";
    private static final String CORE_CHANGE_LOG = "classpath:/com/sap/cloud/lm/sl/cf/core/db/changelog/db-changelog.xml";
    private static final String ENTITY_MANAGER_DEFAULT_PERSISTENCE_UNIT_NAME = "Default";

    @Inject
    @Bean
    public CloudDataSourceFactoryBean dataSource(ApplicationConfiguration applicationConfiguration) {
        CloudDataSourceFactoryBean cloudDataSourceFactoryBean = new CloudDataSourceFactoryBean();
        cloudDataSourceFactoryBean.setServiceName(DATA_SOURCE_SERVICE_NAME);
        cloudDataSourceFactoryBean.setConfiguration(applicationConfiguration);
        return cloudDataSourceFactoryBean;
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
    public DataSourceTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
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
        localContainerEntityManagerFactoryBean.setPackagesToScan("com.sap.cloud.lm.sl");
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
        return getLiquibaseTemplate(dataSource, PERSISTENCE_CHANGE_LOG);
    }

    private SpringLiquibase getLiquibaseTemplate(DataSource dataSource, String changeLog) {
        SpringLiquibase springLiquibase = new SpringLiquibase();
        springLiquibase.setDataSource(dataSource);
        springLiquibase.setChangeLog(changeLog);
        return springLiquibase;
    }

    @Inject
    @Bean
    @DependsOn("persistenceChangelog")
    public SpringLiquibase coreChangelog(DataSource dataSource) {
        return getLiquibaseTemplate(dataSource, CORE_CHANGE_LOG);
    }

    @Bean
    public IndexProcessIdsOfProgressMessagesChange indexProcessIdsOfProgressMessagesChange() {
        return new IndexProcessIdsOfProgressMessagesChange();
    }

    @Bean
    public IndexSpaceOfLmSlPersistenceFileChange indexSpaceOfLmSlPersistenceChange() {
        return new IndexSpaceOfLmSlPersistenceFileChange();
    }

    @Bean
    public IndexSpaceIdOfOperationChange indexSpaceIdOfOperationChange() {
        return new IndexSpaceIdOfOperationChange();
    }

}

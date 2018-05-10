package com.sap.cloud.lm.sl.cf.web.ds;

import javax.sql.DataSource;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudException;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.service.PooledServiceConnectorConfig.PoolConfig;
import org.springframework.cloud.service.relational.DataSourceConfig;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;

public class CloudDataSourceFactoryBean implements FactoryBean<DataSource>, InitializingBean {

    private String serviceName;
    private DataSource defaultDataSource;
    private DataSource dataSource;
    private ApplicationConfiguration configuration;

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setDefaultDataSource(DataSource dataSource) {
        this.defaultDataSource = dataSource;
    }

    public void setConfiguration(ApplicationConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void afterPropertiesSet() {
        DataSource ds = getCloudDataSource(serviceName);
        dataSource = (ds != null) ? ds : defaultDataSource;
    }

    @Override
    public DataSource getObject() {
        return dataSource;
    }

    @Override
    public Class<?> getObjectType() {
        return DataSource.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    private DataSource getCloudDataSource(String serviceName) {
        DataSource dataSource = null;
        try {
            if (serviceName != null && !serviceName.isEmpty()) {
                int maxPoolSize = configuration.getDbConnectionThreads();
                DataSourceConfig config = new DataSourceConfig(new PoolConfig(maxPoolSize, 30000), null);
                dataSource = getSpringCloud().getServiceConnector(serviceName, DataSource.class, config);
            }
        } catch (CloudException e) {
            // Do nothing
        }
        return dataSource;
    }

    protected Cloud getSpringCloud() {
        return new CloudFactory().getCloud();
    }
}

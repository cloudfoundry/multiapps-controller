package org.cloudfoundry.multiapps.controller.web.configuration.bean.factory;

import javax.sql.DataSource;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudException;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.service.PooledServiceConnectorConfig.PoolConfig;
import org.springframework.cloud.service.relational.DataSourceConfig;

public class CloudDataSourceFactoryBean implements FactoryBean<DataSource>, InitializingBean {

    private static final int MAX_WAIT_TIME = 60 * 1000;

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
        DataSource cloudDataSource = null;
        try {
            if (serviceName != null && !serviceName.isEmpty()) {
                int maxPoolSize = configuration.getDbConnectionThreads();
                DataSourceConfig config = new DataSourceConfig(new PoolConfig(maxPoolSize, MAX_WAIT_TIME), null);
                cloudDataSource = getSpringCloud().getServiceConnector(serviceName, DataSource.class, config);
            }
        } catch (CloudException e) {
            // Do nothing
        }
        return cloudDataSource;
    }

    protected Cloud getSpringCloud() {
        return new CloudFactory().getCloud();
    }
}

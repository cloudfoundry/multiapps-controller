package com.sap.cloud.lm.sl.cf.web.configuration.bean.factory;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudException;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.service.PooledServiceConnectorConfig.PoolConfig;
import org.springframework.cloud.service.relational.DataSourceConfig;

import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;

public class CloudDataSourceFactoryBean implements FactoryBean<DataSource>, InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudDataSourceFactoryBean.class);

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
                Cloud springCloud = getSpringCloud();
                dataSource = springCloud.getServiceConnector(serviceName, DataSource.class, config);
            }
        } catch (CloudException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return dataSource;
    }

    protected Cloud getSpringCloud() {
        return new CloudFactory().getCloud();
    }
}

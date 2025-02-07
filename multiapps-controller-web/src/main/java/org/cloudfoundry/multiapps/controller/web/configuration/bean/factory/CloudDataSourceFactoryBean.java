package org.cloudfoundry.multiapps.controller.web.configuration.bean.factory;

import javax.sql.DataSource;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationInstanceNameUtil;
import org.cloudfoundry.multiapps.controller.persistence.util.DataSourceFactory;
import org.cloudfoundry.multiapps.controller.persistence.util.EnvironmentServicesFinder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import io.pivotal.cfenv.jdbc.CfJdbcService;

public class CloudDataSourceFactoryBean implements FactoryBean<DataSource>, InitializingBean {

    private final String serviceName;
    private final DataSourceFactory dataSourceFactory;
    private final EnvironmentServicesFinder environmentServicesFinder;
    private final ApplicationConfiguration applicationConfiguration;
    private DataSource dataSource;

    public CloudDataSourceFactoryBean(String serviceName, DataSourceFactory dataSourceFactory,
                                      EnvironmentServicesFinder environmentServicesFinder,
                                      ApplicationConfiguration applicationConfiguration) {
        this.serviceName = serviceName;
        this.dataSourceFactory = dataSourceFactory;
        this.environmentServicesFinder = environmentServicesFinder;
        this.applicationConfiguration = applicationConfiguration;
    }

    @Override
    public void afterPropertiesSet() {
        this.dataSource = createDataSource(serviceName);
    }

    @Override
    public Class<DataSource> getObjectType() {
        return DataSource.class;
    }

    @Override
    public DataSource getObject() {
        return dataSource;
    }

    private DataSource createDataSource(String serviceName) {
        CfJdbcService service = environmentServicesFinder.findJdbcService(serviceName);
        if (service == null) {
            return null;
        }
        return dataSourceFactory.createDataSource(service, applicationConfiguration.getDbConnectionThreads(),
                                                  ApplicationInstanceNameUtil.buildApplicationInstanceTemplate(applicationConfiguration));
    }

}

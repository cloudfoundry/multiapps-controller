package org.cloudfoundry.multiapps.controller.web.configuration.bean.factory;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.pivotal.cfenv.jdbc.CfJdbcService;

@Named
public class DataSourceFactory {

    private final ApplicationConfiguration configuration;

    @Inject
    public DataSourceFactory(ApplicationConfiguration configuration) {
        this.configuration = configuration;
    }

    public HikariDataSource createHikariDataSource(CfJdbcService service) {
        return new HikariDataSource(createHikariConfig(service));
    }

    private HikariConfig createHikariConfig(CfJdbcService service) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setUsername(service.getUsername());
        hikariConfig.setPassword(service.getPassword());
        hikariConfig.setJdbcUrl(service.getJdbcUrl());
        hikariConfig.setConnectionTimeout(60000);
        hikariConfig.setMaximumPoolSize(configuration.getDbConnectionThreads());
        return hikariConfig;
    }

}

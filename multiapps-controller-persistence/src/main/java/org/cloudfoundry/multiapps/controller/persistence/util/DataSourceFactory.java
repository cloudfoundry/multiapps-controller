package org.cloudfoundry.multiapps.controller.persistence.util;

import javax.inject.Named;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.pivotal.cfenv.jdbc.CfJdbcService;

@Named
public class DataSourceFactory {

    public DataSource createDataSource(CfJdbcService service) {
        return createDataSource(service, null);
    }

    public DataSource createDataSource(CfJdbcService service, Integer maximumPoolSize) {
        return new HikariDataSource(createHikariConfig(service, maximumPoolSize));
    }

    private HikariConfig createHikariConfig(CfJdbcService service, Integer maximumPoolSize) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setUsername(service.getUsername());
        hikariConfig.setPassword(service.getPassword());
        hikariConfig.setJdbcUrl(service.getJdbcUrl());
        hikariConfig.setConnectionTimeout(60000);
        hikariConfig.setIdleTimeout(60000);
        hikariConfig.setMinimumIdle(10);
        hikariConfig.addDataSourceProperty("tcpKeepAlive", true);
        if (maximumPoolSize != null) {
            hikariConfig.setMaximumPoolSize(maximumPoolSize);
        }
        hikariConfig.setRegisterMbeans(true);
        return hikariConfig;
    }

}

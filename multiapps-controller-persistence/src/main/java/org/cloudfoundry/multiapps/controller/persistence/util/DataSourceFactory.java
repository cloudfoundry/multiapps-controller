package org.cloudfoundry.multiapps.controller.persistence.util;

import java.nio.file.Path;

import javax.inject.Named;
import javax.sql.DataSource;

import org.cloudfoundry.multiapps.controller.persistence.Constants;

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
        hikariConfig.setDriverClassName(org.postgresql.Driver.class.getName());

        configureSSLClientKeyIfExists(service, hikariConfig);

        if (maximumPoolSize != null) {
            hikariConfig.setMaximumPoolSize(maximumPoolSize);
        }
        hikariConfig.setRegisterMbeans(true);
        return hikariConfig;
    }

    private void configureSSLClientKeyIfExists(CfJdbcService service, HikariConfig hikariConfig) {
        String clientKey = (String) service.getCredentials()
                                           .getMap()
                                           .get("sslkey");
        if (clientKey != null) {
            configureClientCertificate(clientKey, hikariConfig);
        }
    }

    private void configureClientCertificate(String clientKey, HikariConfig hikariConfig) {
        ClientKeyConfigurationHandler sslKeyHandler = new ClientKeyConfigurationHandler();
        Path encodedKeyPath = sslKeyHandler.createEncodedKeyFile(clientKey, Constants.SSL_CLIENT_KEY_FILE_NAME);
        hikariConfig.addDataSourceProperty("sslkey", encodedKeyPath.toAbsolutePath());
    }

}

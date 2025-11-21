package org.cloudfoundry.multiapps.controller.persistence.util;

import java.nio.file.Path;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.pivotal.cfenv.jdbc.CfJdbcService;
import jakarta.inject.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.cloudfoundry.multiapps.controller.persistence.dto.DatabaseServiceKey;

@Named
public class DataSourceFactory {

    private static final Logger LOGGER = (Logger) LogManager.getLogger(DataSourceFactory.class);

    public DataSource createDataSource(CfJdbcService service) {
        return createDataSource(service, null, null);
    }

    public DataSource createDataSource(DatabaseServiceKey databaseServiceKey) {
        DatabaseConfig databaseConfig = createDatabaseConfigFromDatabaseServiceKey(databaseServiceKey);
        return new HikariDataSource(createHikariConfig(databaseConfig, null, "null"));
    }

    public DataSource createDataSource(CfJdbcService service, Integer maximumPoolSize, String appInstanceTemplate) {
        DatabaseConfig databaseConfig = createDatabaseConfigFromCfJdbcService(service);
        return new HikariDataSource(createHikariConfig(databaseConfig, maximumPoolSize, appInstanceTemplate));
    }

    private HikariConfig createHikariConfig(DatabaseConfig databaseConfig, Integer maximumPoolSize, String appInstanceTemplate) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setUsername(databaseConfig.username);
        hikariConfig.setPassword(databaseConfig.password);
        hikariConfig.setJdbcUrl(databaseConfig.jdbcUrl);
        hikariConfig.setConnectionTimeout(60000);
        hikariConfig.setIdleTimeout(60000);
        hikariConfig.setMinimumIdle(10);
        hikariConfig.addDataSourceProperty("tcpKeepAlive", true);

        configureSSLClientKeyIfExists(databaseConfig, hikariConfig);

        if (appInstanceTemplate != null) {
            hikariConfig.addDataSourceProperty("ApplicationName", appInstanceTemplate);
        }
        if (maximumPoolSize != null) {
            hikariConfig.setMaximumPoolSize(maximumPoolSize);
        }
        hikariConfig.setRegisterMbeans(true);
        return hikariConfig;
    }

    private void configureSSLClientKeyIfExists(DatabaseConfig service, HikariConfig hikariConfig) {
        if (service.sslKey != null) {
            configureClientCertificate(service.sslKey, hikariConfig);
        }
    }

    private void configureClientCertificate(String clientKey, HikariConfig hikariConfig) {
        ClientKeyConfigurationHandler sslKeyHandler = new ClientKeyConfigurationHandler();
        Path encodedKeyPath = sslKeyHandler.createEncodedKeyFile(clientKey, Constants.SSL_CLIENT_KEY_FILE_NAME);
        hikariConfig.addDataSourceProperty("sslkey", encodedKeyPath.toAbsolutePath());
    }

    private DatabaseConfig createDatabaseConfigFromCfJdbcService(CfJdbcService service) {
        String clientKey = (String) service.getCredentials()
                                           .getMap()
                                           .get("sslkey");
        return new DatabaseConfig(service.getUsername(), service.getPassword(), service.getJdbcUrl(), clientKey);
    }

    private DatabaseConfig createDatabaseConfigFromDatabaseServiceKey(DatabaseServiceKey databaseServiceKey) {
        return new DatabaseConfig(databaseServiceKey.getUsername(), databaseServiceKey.getPassword(), databaseServiceKey.getJdbcUri(),
                                  null);
    }

    private record DatabaseConfig(String username, String password, String jdbcUrl, String sslKey) {
    }

}

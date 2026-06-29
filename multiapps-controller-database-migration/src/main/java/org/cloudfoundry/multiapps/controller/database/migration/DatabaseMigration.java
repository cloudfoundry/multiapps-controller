package org.cloudfoundry.multiapps.controller.database.migration;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.database.migration.executor.DatabaseSequenceMigrationExecutor;
import org.cloudfoundry.multiapps.controller.database.migration.executor.DatabaseTableMigrationExecutor;
import org.cloudfoundry.multiapps.controller.database.migration.executor.ImmutableDatabaseSequenceMigrationExecutor;
import org.cloudfoundry.multiapps.controller.database.migration.executor.ImmutableDatabaseTableMigrationExecutor;
import org.cloudfoundry.multiapps.controller.database.migration.extractor.DataSourceEnvironmentExtractor;
import org.cloudfoundry.multiapps.controller.persistence.dto.DatabaseServiceKey;

public class DatabaseMigration {

    private static final Logger LOGGER = (Logger) LogManager.getLogger(DatabaseMigration.class);
    private static final String DATABASE_TARGET_SERVICE_KEY = "DATABASE_TARGET_SERVICE_KEY";
    private static final String TARGET_DATABASE_SERVICE_NAME = "deploy-service-database";

    private static final List<String> SEQUENCES_TO_MIGRATE = List.of("configuration_entry_sequence",
                                                                     "configuration_subscription_sequence",
                                                                     "backup_descriptor_sequence");

    private static final List<String> TABLES_TO_MIGRATE = List.of("configuration_registry",
                                                                  "configuration_subscription",
                                                                  "backup_descriptor");

    public static void main(String[] args) {
        configureLogger();

        DataSource sourceDataSource = extractSourceDataSource();
        DataSource targetDataSource = extractTargetDataSource();

        migrateSequences(sourceDataSource, targetDataSource);
        migrateTables(sourceDataSource, targetDataSource);

        LOGGER.info("Database migration completed.");
    }

    private static DataSource extractSourceDataSource() {
        DatabaseServiceKey serviceKey = getServiceKeyFromEnvironment();
        return new DataSourceEnvironmentExtractor().extractDataSource(serviceKey);
    }

    private static DataSource extractTargetDataSource() {
        return new DataSourceEnvironmentExtractor().extractDataSource(TARGET_DATABASE_SERVICE_NAME);
    }

    private static void migrateSequences(DataSource sourceDataSource, DataSource targetDataSource) {
        DatabaseSequenceMigrationExecutor executor = ImmutableDatabaseSequenceMigrationExecutor.builder()
                                                                                               .sourceDataSource(sourceDataSource)
                                                                                               .targetDataSource(targetDataSource)
                                                                                               .build();
        SEQUENCES_TO_MIGRATE.forEach(executor::executeMigration);
    }

    private static void migrateTables(DataSource sourceDataSource, DataSource targetDataSource) {
        DatabaseTableMigrationExecutor executor = ImmutableDatabaseTableMigrationExecutor.builder()
                                                                                         .sourceDataSource(sourceDataSource)
                                                                                         .targetDataSource(targetDataSource)
                                                                                         .build();
        TABLES_TO_MIGRATE.forEach(executor::executeMigration);
    }

    private static DatabaseServiceKey getServiceKeyFromEnvironment() {
        String databaseTargetServiceKey = System.getenv(DATABASE_TARGET_SERVICE_KEY);
        return new DatabaseServiceKey(JsonUtil.convertJsonToMap(databaseTargetServiceKey));
    }

    private static void configureLogger() {
        ClassLoader classLoader = DatabaseMigration.class.getClassLoader();
        if (classLoader == null) {
            return;
        }
        try (InputStream inputStream = classLoader.getResourceAsStream("console-logger.properties");
            LoggerContext loggerContext = new LoggerContext("DatabaseMigration")) {
            if (inputStream != null) {
                ConfigurationSource configSource = new ConfigurationSource(inputStream);
                loggerContext.setConfigLocation(configSource.getURI());
            }
        } catch (IOException e) {
            // Using System.out.println() instead of LOGGER.warn(), because logging is likely not configured due to the exception.
            System.out.println("An error occurred while trying to configure logging: " + e.getMessage());
            e.printStackTrace();
        }
    }

}

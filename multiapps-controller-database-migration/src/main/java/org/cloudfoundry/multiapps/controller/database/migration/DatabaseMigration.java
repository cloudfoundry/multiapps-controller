package org.cloudfoundry.multiapps.controller.database.migration;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Arrays;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.cloudfoundry.multiapps.controller.database.migration.executor.DatabaseSequenceMigrationExecutor;
import org.cloudfoundry.multiapps.controller.database.migration.executor.DatabaseTableMigrationExecutor;
import org.cloudfoundry.multiapps.controller.database.migration.executor.ImmutableDatabaseSequenceMigrationExecutor;
import org.cloudfoundry.multiapps.controller.database.migration.executor.ImmutableDatabaseTableMigrationExecutor;
import org.cloudfoundry.multiapps.controller.database.migration.extractor.DataSourceEnvironmentExtractor;

public class DatabaseMigration {

    private final static Logger LOGGER = Logger.getLogger(DatabaseMigration.class);

    public static void main(String[] args) throws SQLException {
        configureLogger();
        LOGGER.info("Starting database migration...");
        DataSourceEnvironmentExtractor environmentExtractor = new DataSourceEnvironmentExtractor();
        DataSource sourceDataSource = environmentExtractor.extractDataSource("deploy-service-database-source");
        DataSource targetDataSource = environmentExtractor.extractDataSource("deploy-service-database");

        DatabaseSequenceMigrationExecutor sequenceMigrationExecutor = ImmutableDatabaseSequenceMigrationExecutor.builder()
                                                                                                                .sourceDataSource(sourceDataSource)
                                                                                                                .targetDataSource(targetDataSource)
                                                                                                                .build();

        DatabaseTableMigrationExecutor tableMigrationExecutor = ImmutableDatabaseTableMigrationExecutor.builder()
                                                                                                       .sourceDataSource(sourceDataSource)
                                                                                                       .targetDataSource(targetDataSource)
                                                                                                       .build();
        Arrays.asList("configuration_entry_sequence", "configuration_subscription_sequence")
              .stream()
              .forEach(sequenceMigrationExecutor::executeMigration);

        Arrays.asList("configuration_registry", "configuration_subscription")
              .stream()
              .forEach(tableMigrationExecutor::executeMigration);

        LOGGER.info("Database migration completed.");
    }

    private static void configureLogger() {
        try (InputStream inputStream = DatabaseMigration.class.getClassLoader()
                                                              .getResourceAsStream("console-logger.properties")) {
            if (inputStream != null) {
                PropertyConfigurator.configure(inputStream);
            }
        } catch (IOException e) {
            LOGGER.warn("There was an error trying to configure the logger.", e);
            LOGGER.info("Proceeding with default logger configuration.");
        }
    }

}

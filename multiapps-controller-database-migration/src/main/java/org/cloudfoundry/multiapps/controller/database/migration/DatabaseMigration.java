package org.cloudfoundry.multiapps.controller.database.migration;

import java.io.IOException;
import java.io.InputStream;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.cloudfoundry.multiapps.controller.database.migration.executor.DatabaseSequenceMigrationExecutor;
import org.cloudfoundry.multiapps.controller.database.migration.executor.DatabaseTableMigrationExecutor;
import org.cloudfoundry.multiapps.controller.database.migration.executor.ImmutableDatabaseSequenceMigrationExecutor;
import org.cloudfoundry.multiapps.controller.database.migration.executor.ImmutableDatabaseTableMigrationExecutor;
import org.cloudfoundry.multiapps.controller.database.migration.extractor.DataSourceEnvironmentExtractor;

public class DatabaseMigration {

    private static final Logger LOGGER = Logger.getLogger(DatabaseMigration.class);

    public static void main(String[] args) {
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
        sequenceMigrationExecutor.executeMigration("configuration_entry_sequence");
        sequenceMigrationExecutor.executeMigration("configuration_subscription_sequence");

        tableMigrationExecutor.executeMigration("configuration_registry");
        tableMigrationExecutor.executeMigration("configuration_subscription");

        LOGGER.info("Database migration completed.");
    }

    private static void configureLogger() {
        ClassLoader classLoader = DatabaseMigration.class.getClassLoader();
        if (classLoader != null) {
            try (InputStream inputStream = classLoader.getResourceAsStream("console-logger.properties")) {
                if (inputStream != null) {
                    PropertyConfigurator.configure(inputStream);
                }
            } catch (IOException e) {
                // Using System.out.println() instead of LOGGER.warn(), because logging is likely not configured due to the exception.
                System.out.println("An error occurred while trying to configure logging: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

}

package org.cloudfoundry.multiapps.controller.persistence.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.MigrationFailedException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

public class TestDataSourceProvider {

    public static DataSource getDataSource(String liquibaseChangelogLocation) throws Exception {
        // create a hsql in memory connection
        Connection connection = createH2InMemory();

        // Create the schema for unit testing
        Database liquibaseDb = DatabaseFactory.getInstance()
                                              .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        Liquibase lq = new Liquibase(liquibaseChangelogLocation, new ClassLoaderResourceAccessor(), liquibaseDb);
        try {
            lq.update("");
        } catch (MigrationFailedException e) {
            // catch the exception because in PopulateConfigurationRegistrySpaceIdColumnChange liquibase change there is rest call
            if (e.getCause()
                 .getClass() != UnexpectedLiquibaseException.class) {
                throw e;
            }
        }

        // Initialize the fileService to use our in-memory connection through a pool emulation (so
        // that close releases rather than close)
        return new SingleConnectionDataSource(connection, true);
    }

    private static Connection createH2InMemory() throws SQLException {
        Connection connection = null;
        connection = DriverManager.getConnection("jdbc:h2:mem:testdb", "sa", "");
        return connection;
    }
}

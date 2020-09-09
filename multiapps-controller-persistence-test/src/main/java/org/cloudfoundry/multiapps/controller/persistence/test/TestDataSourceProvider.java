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
import liquibase.resource.ClassLoaderResourceAccessor;

public final class TestDataSourceProvider {

    private TestDataSourceProvider() {

    }

    public static DataSource getDataSource(String liquibaseChangelogLocation) throws Exception {
        // Liquibase closes the connection after it's done, so we need a separate one for it.
        Connection connectionForLiquibase = createH2InMemoryConnection();
        Connection connection = createH2InMemoryConnection();

        initializeDataSource(liquibaseChangelogLocation, connectionForLiquibase);

        return new SingleConnectionDataSource(connection, true);
    }

    private static void initializeDataSource(String liquibaseChangelogLocation, Connection connection) throws Exception {
        Database liquibaseDatabase = DatabaseFactory.getInstance()
                                                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        try (Liquibase liquibase = new Liquibase(liquibaseChangelogLocation, new ClassLoaderResourceAccessor(), liquibaseDatabase)) {
            liquibase.update("");
        }
    }

    private static Connection createH2InMemoryConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:testdb", "sa", "");
    }

}

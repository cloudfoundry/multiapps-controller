package org.cloudfoundry.multiapps.controller.web.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.time.Duration;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.postgresql.Bucket4jPostgreSQL;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;

/**
 * Proves the real PostgreSQL SELECT ... FOR UPDATE round-trip behind {@link PostgresBucketStore}: token consumption is coordinated through
 * the shared database, so two controller instances pointed at the same {@code operation_rate_limit_bucket} table drain a single logical
 * bucket. A real PostgreSQL instance is required, so this runs under failsafe (name ends in {@code IntegrationTest}) and needs Docker.
 */
@Testcontainers
class OperationRateLimiterIntegrationTest {

    private static final String BUCKET_TABLE_NAME = "operation_rate_limit_bucket";
    // Step 1's changeset lives in this changelog; running it here validates the exact DDL the application uses.
    private static final String BUCKET_CHANGELOG_LOCATION = "org/cloudfoundry/multiapps/controller/persistence/db/changelog/db-changelog-2.52.0-persistence.xml";
    // The master db-changelog.xml resolves this property per-dbms; on PostgreSQL it maps to BYTEA. We supply it directly because we run only
    // the 2.52.0 changelog, not the master that declares the property.
    private static final String SMALL_BLOB_TYPE_PARAMETER = "small-blob.type";
    private static final String POSTGRES_SMALL_BLOB_TYPE = "BYTEA";

    private static final long BUCKET_CAPACITY = 3;
    private static final long TOKENS_PER_CONSUME = 1;
    // A refill window far larger than the test runtime guarantees no tokens are replenished mid-test, keeping the capacity assertions
    // deterministic without any sleeps.
    private static final Duration REFILL_WINDOW = Duration.ofHours(1);

    private static final long OPERATION_KEY = 42L;
    private static final long OTHER_OPERATION_KEY = 43L;

    @Container
    private final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    private DataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        dataSource = createDataSource();
        applyBucketSchema();
    }

    private DataSource createDataSource() {
        var pgDataSource = new PGSimpleDataSource();
        pgDataSource.setUrl(postgres.getJdbcUrl());
        pgDataSource.setUser(postgres.getUsername());
        pgDataSource.setPassword(postgres.getPassword());
        return pgDataSource;
    }

    private void applyBucketSchema() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            Database database = DatabaseFactory.getInstance()
                                               .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (Liquibase liquibase = new Liquibase(BUCKET_CHANGELOG_LOCATION, new ClassLoaderResourceAccessor(), database)) {
                liquibase.setChangeLogParameter(SMALL_BLOB_TYPE_PARAMETER, POSTGRES_SMALL_BLOB_TYPE);
                liquibase.update("");
            }
        }
    }

    @Test
    void testConsumingAcrossTwoInstancesDrainsSharedBucket() {
        var firstInstance = buildProxyManager();
        var secondInstance = buildProxyManager();

        // Drain the shared bucket by splitting the capacity across two independent proxy managers over the same database.
        assertTrue(consumeOneToken(firstInstance, OPERATION_KEY), "first token (instance one) should be consumed");
        assertTrue(consumeOneToken(secondInstance, OPERATION_KEY), "second token (instance two) should be consumed");
        assertTrue(consumeOneToken(firstInstance, OPERATION_KEY), "third token (instance one) should be consumed");

        // The bucket is now empty. A further consume from EITHER instance must be rejected, proving the SELECT ... FOR UPDATE state is
        // shared through Postgres rather than held per-instance.
        assertFalse(consumeOneToken(secondInstance, OPERATION_KEY), "instance two must see the shared bucket as drained");
        assertFalse(consumeOneToken(firstInstance, OPERATION_KEY), "instance one must see the shared bucket as drained");
    }

    @Test
    void testDifferentKeysAreIndependentBuckets() {
        var firstInstance = buildProxyManager();
        var secondInstance = buildProxyManager();

        for (var token = 0; token < BUCKET_CAPACITY; token++) {
            assertTrue(consumeOneToken(firstInstance, OPERATION_KEY), "draining the bucket for the first key should succeed");
        }
        assertFalse(consumeOneToken(firstInstance, OPERATION_KEY), "the first key's bucket should be drained");

        // A different key resolves to a different row, so its bucket is untouched even from the other instance.
        assertTrue(consumeOneToken(secondInstance, OTHER_OPERATION_KEY), "a different key must have its own independent bucket");
    }

    @Test
    void testBucketStateRowIsPersistedWithExpiresAt() throws Exception {
        var proxyManager = buildProxyManager();

        assertTrue(consumeOneToken(proxyManager, OPERATION_KEY), "the first consume should persist bucket state");

        assertBucketRowPersisted(OPERATION_KEY);
    }

    private ProxyManager<Long> buildProxyManager() {
        return Bucket4jPostgreSQL.selectForUpdateBasedBuilder(dataSource)
                                 .table(BUCKET_TABLE_NAME)
                                 .build();
    }

    private boolean consumeOneToken(ProxyManager<Long> proxyManager, long key) {
        BucketProxy bucket = proxyManager.getProxy(key, this::buildBucketConfiguration);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(TOKENS_PER_CONSUME);
        return probe.isConsumed();
    }

    private BucketConfiguration buildBucketConfiguration() {
        Bandwidth bandwidth = Bandwidth.builder()
                                       .capacity(BUCKET_CAPACITY)
                                       .refillGreedy(BUCKET_CAPACITY, REFILL_WINDOW)
                                       .build();
        return BucketConfiguration.builder()
                                  .addLimit(bandwidth)
                                  .build();
    }

    private void assertBucketRowPersisted(long key) throws Exception {
        var selectRow = "SELECT state, expires_at FROM " + BUCKET_TABLE_NAME + " WHERE id = ?";
        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement(selectRow)) {
            statement.setLong(1, key);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next(), "a bucket row must be persisted for the consumed key");
                assertNotNull(resultSet.getBytes("state"), "the serialized bucket state must be stored");
                var expiresAt = resultSet.getLong("expires_at");
                assertFalse(resultSet.wasNull(), "expires_at must be populated so bucket4j can expire stale rows");
                assertTrue(expiresAt > 0, "expires_at must be a positive epoch value");
                assertFalse(resultSet.next(), "a single key must map to exactly one bucket row");
            }
        }
        assertEquals(1, countBucketRows(), "only the consumed key's bucket row should exist");
    }

    private long countBucketRows() throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.prepareStatement("SELECT COUNT(*) FROM " + BUCKET_TABLE_NAME);
             ResultSet resultSet = statement.executeQuery()) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }
}

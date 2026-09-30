package org.cloudfoundry.multiapps.controller.process.util;

import java.time.Duration;

import javax.sql.DataSource;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.postgresql.Bucket4jPostgreSQL;
import io.github.bucket4j.postgresql.PostgreSQLSelectForUpdateBasedProxyManager;

/**
 * {@link BucketStore} backed by a PostgreSQL {@link ProxyManager} that uses SELECT ... FOR UPDATE row locking to coordinate token
 * consumption across all controller instances sharing the database.
 */
@Named
public class PostgresBucketStore implements BucketStore {

    private static final String BUCKET_TABLE_NAME = "operation_rate_limit_bucket";
    private static final Duration BUCKET_TIME_TO_LIVE = Duration.ofHours(1);

    private final PostgreSQLSelectForUpdateBasedProxyManager<Long> proxyManager;

    @Inject
    public PostgresBucketStore(DataSource dataSource) {
        this.proxyManager = Bucket4jPostgreSQL.selectForUpdateBasedBuilder(dataSource)
                                              .table(BUCKET_TABLE_NAME)
                                              .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(BUCKET_TIME_TO_LIVE))
                                              .build();
    }

    @Override
    public Bucket getBucket(long key, BucketConfiguration configuration) {
        return proxyManager.getProxy(key, () -> configuration);
    }

    @Override
    public int removeExpiredEntries(int batchSize) {
        return proxyManager.removeExpired(batchSize);
    }
}

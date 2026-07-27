package org.cloudfoundry.multiapps.controller.web.util;

import javax.sql.DataSource;

import jakarta.inject.Named;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.postgresql.Bucket4jPostgreSQL;

/**
 * {@link BucketStore} backed by a PostgreSQL {@link ProxyManager} that uses SELECT ... FOR UPDATE row locking to coordinate token
 * consumption across all controller instances sharing the database.
 */
@Named
public class PostgresBucketStore implements BucketStore {

    private static final String BUCKET_TABLE_NAME = "operation_rate_limit_bucket";

    private final ProxyManager<Long> proxyManager;

    public PostgresBucketStore(DataSource dataSource) {
        this.proxyManager = Bucket4jPostgreSQL.selectForUpdateBasedBuilder(dataSource)
                                              .table(BUCKET_TABLE_NAME)
                                              .build();
    }

    @Override
    public Bucket getBucket(long key, BucketConfiguration configuration) {
        return proxyManager.getProxy(key, () -> configuration);
    }
}

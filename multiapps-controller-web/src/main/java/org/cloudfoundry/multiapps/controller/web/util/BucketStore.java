package org.cloudfoundry.multiapps.controller.web.util;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;

/**
 * Resolves distributed token buckets by key. Abstracting this behind an interface keeps the concrete bucket4j proxy manager (and its backing
 * data store) out of the rate limiter, so unit tests can supply mocked buckets without touching a real database.
 */
public interface BucketStore {

    Bucket getBucket(long key, BucketConfiguration configuration);

    int removeExpiredEntries(int batchSize);
}

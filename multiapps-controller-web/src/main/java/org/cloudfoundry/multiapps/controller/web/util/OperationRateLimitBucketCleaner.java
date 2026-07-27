package org.cloudfoundry.multiapps.controller.web.util;

import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.web.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Periodically deletes expired rows from the operation rate limit bucket table. bucket4j populates each row's expiry but never removes
 * expired rows on its own, so without this sweeper the table grows without bound as new spaces and users start operations.
 */
@Named
public class OperationRateLimitBucketCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationRateLimitBucketCleaner.class);
    private static final int SELECTED_INSTANCE_FOR_CLEAN_UP = 0;
    private static final int DELETE_BATCH_SIZE = 100;
    private static final int MAX_ITERATIONS = 1000;

    private final ApplicationConfiguration applicationConfiguration;
    private final BucketStore bucketStore;

    @Inject
    public OperationRateLimitBucketCleaner(ApplicationConfiguration applicationConfiguration, BucketStore bucketStore) {
        this.applicationConfiguration = applicationConfiguration;
        this.bucketStore = bucketStore;
    }

    @Scheduled(fixedRate = 1, timeUnit = TimeUnit.HOURS)
    public void cleanUpExpiredBuckets() {
        if (!applicationConfiguration.isOperationRateLimitingEnabled()) {
            return;
        }
        if (applicationConfiguration.getApplicationInstanceIndex() != SELECTED_INSTANCE_FOR_CLEAN_UP) {
            return;
        }
        LOGGER.info(Messages.STARTING_CLEAN_UP_OF_EXPIRED_OPERATION_RATE_LIMIT_BUCKETS);
        try {
            int totalDeleted = deleteExpiredBucketsInBatches();
            LOGGER.info(MessageFormat.format(Messages.DELETED_EXPIRED_OPERATION_RATE_LIMIT_BUCKETS_0, totalDeleted));
        } catch (Exception e) {
            LOGGER.error(Messages.COULD_NOT_CLEAN_UP_EXPIRED_OPERATION_RATE_LIMIT_BUCKETS, e);
        }
    }

    private int deleteExpiredBucketsInBatches() {
        int totalDeleted = 0;
        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            int deleted = bucketStore.removeExpiredEntries(DELETE_BATCH_SIZE);
            totalDeleted += deleted;
            if (deleted < DELETE_BATCH_SIZE) {
                break;
            }
        }
        return totalDeleted;
    }
}

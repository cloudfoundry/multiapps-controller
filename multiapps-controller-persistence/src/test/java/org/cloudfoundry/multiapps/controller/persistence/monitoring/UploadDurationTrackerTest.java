package org.cloudfoundry.multiapps.controller.persistence.monitoring;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.cloudfoundry.multiapps.controller.persistence.monitoring.UploadDurationTracker.UploadPathStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadDurationTrackerTest {

    private UploadDurationTracker uploadDurationTracker;

    @BeforeEach
    void setUp() {
        uploadDurationTracker = new UploadDurationTracker();
    }

    @Test
    void testRecordAppBinaryUploadIncrementsCountAndSum() {
        uploadDurationTracker.recordAppBinaryUpload(100, false);
        uploadDurationTracker.recordAppBinaryUpload(250, false);

        UploadPathStatistics statistics = uploadDurationTracker.getAppBinaryStatistics();
        assertEquals(2, statistics.totalCount());
        assertEquals(350, statistics.sumDurationMs());
    }

    @Test
    void testRecordObjectStoreUploadIncrementsCountAndSum() {
        uploadDurationTracker.recordObjectStoreUpload(75, false);
        uploadDurationTracker.recordObjectStoreUpload(125, false);

        UploadPathStatistics statistics = uploadDurationTracker.getObjectStoreStatistics();
        assertEquals(2, statistics.totalCount());
        assertEquals(200, statistics.sumDurationMs());
    }

    @Test
    void testMaxDurationReturnsLargestAcrossRecords() {
        uploadDurationTracker.recordAppBinaryUpload(100, false);
        uploadDurationTracker.recordAppBinaryUpload(500, false);
        uploadDurationTracker.recordAppBinaryUpload(250, false);

        assertEquals(500, uploadDurationTracker.getAppBinaryStatistics()
                                               .maxDurationMs());
    }

    @Test
    void testMaxDurationIsWindowedAndResetsOnRead() {
        uploadDurationTracker.recordAppBinaryUpload(400, false);

        UploadPathStatistics statistics = uploadDurationTracker.getAppBinaryStatistics();
        assertEquals(400, statistics.maxDurationMs());
        assertEquals(0, statistics.maxDurationMs());
    }

    @Test
    void testMaxDurationStartsFreshWindowAfterRead() {
        UploadPathStatistics statistics = uploadDurationTracker.getAppBinaryStatistics();

        uploadDurationTracker.recordAppBinaryUpload(400, false);
        assertEquals(400, statistics.maxDurationMs());

        uploadDurationTracker.recordAppBinaryUpload(150, false);
        assertEquals(150, statistics.maxDurationMs());
    }

    @Test
    void testNegativeDurationIsSetToZeroForSum() {
        uploadDurationTracker.recordAppBinaryUpload(100, false);
        uploadDurationTracker.recordAppBinaryUpload(-500, false);

        UploadPathStatistics statistics = uploadDurationTracker.getAppBinaryStatistics();
        assertEquals(2, statistics.totalCount());
        assertEquals(100, statistics.sumDurationMs());
    }

    @Test
    void testNegativeDurationContributesZeroToMax() {
        uploadDurationTracker.recordAppBinaryUpload(-500, false);

        assertEquals(0, uploadDurationTracker.getAppBinaryStatistics()
                                             .maxDurationMs());
    }

    @Test
    void testTimeoutIncrementsCumulativeAndWindowedCounts() {
        uploadDurationTracker.recordAppBinaryUpload(100, true);
        uploadDurationTracker.recordAppBinaryUpload(200, true);
        uploadDurationTracker.recordAppBinaryUpload(300, false);

        UploadPathStatistics statistics = uploadDurationTracker.getAppBinaryStatistics();
        assertEquals(2, statistics.timeoutCount());
        assertEquals(2, statistics.timeoutsInWindow());
    }

    @Test
    void testTimeoutsInWindowResetsOnReadButTimeoutCountDoesNot() {
        uploadDurationTracker.recordAppBinaryUpload(100, true);

        UploadPathStatistics statistics = uploadDurationTracker.getAppBinaryStatistics();
        assertEquals(1, statistics.timeoutsInWindow());
        assertEquals(0, statistics.timeoutsInWindow());
        assertEquals(1, statistics.timeoutCount());
        assertEquals(1, statistics.timeoutCount());
    }

    @Test
    void testRecordAppBinaryUploadRejectionIncrementsCumulativeAndWindowed() {
        uploadDurationTracker.recordAppBinaryUploadRejection();
        uploadDurationTracker.recordAppBinaryUploadRejection();

        UploadPathStatistics statistics = uploadDurationTracker.getAppBinaryStatistics();
        assertEquals(2, statistics.rejectionCount());
        assertEquals(2, statistics.rejectionsInWindow());
    }

    @Test
    void testRejectionsInWindowResetsOnReadButRejectionCountDoesNot() {
        uploadDurationTracker.recordAppBinaryUploadRejection();

        UploadPathStatistics statistics = uploadDurationTracker.getAppBinaryStatistics();
        assertEquals(1, statistics.rejectionsInWindow());
        assertEquals(0, statistics.rejectionsInWindow());
        assertEquals(1, statistics.rejectionCount());
        assertEquals(1, statistics.rejectionCount());
    }

    @Test
    void testAppBinaryAndObjectStoreStatisticsAreIndependent() {
        uploadDurationTracker.recordAppBinaryUpload(100, true);
        uploadDurationTracker.recordAppBinaryUploadRejection();

        UploadPathStatistics objectStoreStatistics = uploadDurationTracker.getObjectStoreStatistics();
        assertEquals(0, objectStoreStatistics.totalCount());
        assertEquals(0, objectStoreStatistics.sumDurationMs());
        assertEquals(0, objectStoreStatistics.timeoutCount());
        assertEquals(0, objectStoreStatistics.rejectionCount());

        uploadDurationTracker.recordObjectStoreUpload(200, false);

        UploadPathStatistics appBinaryStatistics = uploadDurationTracker.getAppBinaryStatistics();
        assertEquals(1, appBinaryStatistics.totalCount());
        assertEquals(1, appBinaryStatistics.totalCount());
    }

    @Test
    void testConcurrentRecordsAccumulateSafely() throws InterruptedException {
        int threadCount = 100;
        long maxDuration = threadCount;

        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch blockStartLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        try {
            for (int i = 1; i <= threadCount; i++) {
                long duration = i;

                executor.submit(() -> {
                    try {
                        blockStartLatch.await();
                        uploadDurationTracker.recordAppBinaryUpload(duration, false);
                    } catch (InterruptedException e) {
                        Thread.currentThread()
                              .interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            blockStartLatch.countDown();
            assertTrue(doneLatch.await(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        UploadPathStatistics statistics = uploadDurationTracker.getAppBinaryStatistics();
        assertEquals(threadCount, statistics.totalCount());
        assertEquals(maxDuration, statistics.maxDurationMs());
    }

}

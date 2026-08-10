package org.cloudfoundry.multiapps.controller.persistence.monitoring;

import jakarta.inject.Named;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Named
public class UploadDurationTracker {

    private final UploadPathStatistics appBinaryStatistics = new UploadPathStatistics();

    private final UploadPathStatistics objectStoreStatistics = new UploadPathStatistics();

    public void recordAppBinaryUpload(long durationMillis, boolean timedOut) {
        appBinaryStatistics.record(durationMillis, timedOut);
    }

    public void recordObjectStoreUpload(long durationMillis, boolean timedOut) {
        objectStoreStatistics.record(durationMillis, timedOut);
    }

    public void recordAppBinaryUploadRejection() {
        appBinaryStatistics.recordRejection();
    }

    public UploadPathStatistics getAppBinaryStatistics() {
        return this.appBinaryStatistics;
    }

    public UploadPathStatistics getObjectStoreStatistics() {
        return this.objectStoreStatistics;
    }

    public static final class UploadPathStatistics {

        private final LongAdder total = new LongAdder();

        private final LongAdder timeouts = new LongAdder();

        private final LongAdder sumDuration = new LongAdder();

        private final LongAdder rejections = new LongAdder();

        private final AtomicLong rejectionsInWindow = new AtomicLong(0);

        private final AtomicLong maxDuration = new AtomicLong(0);

        private final AtomicLong timeoutsInWindow = new AtomicLong(0);

        public void record(long durationMillis, boolean timedOut) {
            long duration = Math.max(0, durationMillis);
            total.increment();

            if (timedOut) {
                timeouts.increment();
                timeoutsInWindow.incrementAndGet();
            }

            sumDuration.add(duration);
            maxDuration.accumulateAndGet(duration, Math::max);
        }

        public void recordRejection() {
            rejections.increment();
            rejectionsInWindow.incrementAndGet();
        }

        public long totalCount() {
            return total.sum();
        }

        public long timeoutCount() {
            return timeouts.sum();
        }

        public long maxDurationMs() {
            return maxDuration.getAndSet(0);
        }

        public long sumDurationMs() {
            return sumDuration.sum();
        }

        public long timeoutsInWindow() {
            return timeoutsInWindow.getAndSet(0);
        }

        public long rejectionCount() {
            return rejections.sum();
        }

        public long rejectionsInWindow() {
            return rejectionsInWindow.getAndSet(0);
        }

    }

}

package org.cloudfoundry.multiapps.controller.web.monitoring;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.persistence.monitoring.UploadDurationTracker;

@Named
public class UploadDurationMetrics implements UploadMetricsMBean {

    private final UploadDurationTracker tracker;

    private final AppUploaderThreadPoolInformation appUploaderInfo;

    @Inject
    public UploadDurationMetrics(UploadDurationTracker tracker,
                                 AppUploaderThreadPoolInformation appUploaderInfo) {
        this.tracker = tracker;
        this.appUploaderInfo = appUploaderInfo;
    }

    @Override
    public long getAppBinaryUploadTotalCount() {
        return tracker.getAppBinaryStatistics()
                      .totalCount();
    }

    @Override
    public long getAppBinaryUploadTimeoutCount() {
        return tracker.getAppBinaryStatistics()
                      .timeoutCount();
    }

    @Override
    public long getAppBinaryUploadMaxDurationMs() {
        return tracker.getAppBinaryStatistics()
                      .maxDurationMs();
    }

    @Override
    public long getAppBinaryUploadSumDurationMs() {
        return tracker.getAppBinaryStatistics()
                      .sumDurationMs();
    }

    @Override
    public int getAppUploaderActiveThreads() {
        return appUploaderInfo.getActiveThreads();
    }

    @Override
    public int getAppUploaderMaxThreads() {
        return appUploaderInfo.getMaxThreads();
    }

    @Override
    public long getAppUploaderRejectionCount() {
        return tracker.getAppBinaryStatistics()
                      .rejectionCount();
    }

    @Override
    public long getAppUploaderRejectionCountInWindow() {
        return tracker.getAppBinaryStatistics()
                      .rejectionsInWindow();
    }

    @Override
    public long getObjectStoreUploadTotalCount() {
        return tracker.getObjectStoreStatistics()
                      .totalCount();
    }

    @Override
    public long getObjectStoreUploadTimeoutCount() {
        return tracker.getObjectStoreStatistics()
                      .timeoutCount();
    }

    @Override
    public long getObjectStoreUploadMaxDurationMs() {
        return tracker.getObjectStoreStatistics()
                      .maxDurationMs();
    }

    @Override
    public long getObjectStoreUploadSumDurationMs() {
        return tracker.getObjectStoreStatistics()
                      .sumDurationMs();
    }

    @Override
    public long getAppBinaryUploadTimeoutsInWindow() {
        return tracker.getAppBinaryStatistics()
                      .timeoutsInWindow();
    }

    @Override
    public long getObjectStoreUploadTimeoutsInWindow() {
        return tracker.getObjectStoreStatistics()
                      .timeoutsInWindow();
    }

}

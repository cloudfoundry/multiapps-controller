package org.cloudfoundry.multiapps.controller.web.monitoring;

public interface UploadMetricsMBean {

    long getAppBinaryUploadTotalCount();

    long getAppBinaryUploadTimeoutCount();

    long getAppBinaryUploadMaxDurationMs();

    long getAppBinaryUploadSumDurationMs();

    int getAppUploaderActiveThreads();

    int getAppUploaderMaxThreads();

    long getAppUploaderRejectionCount();

    long getAppUploaderRejectionCountInWindow();

    long getObjectStoreUploadTotalCount();

    long getObjectStoreUploadTimeoutCount();

    long getObjectStoreUploadMaxDurationMs();

    long getObjectStoreUploadSumDurationMs();

    long getAppBinaryUploadTimeoutsInWindow();

    long getObjectStoreUploadTimeoutsInWindow();

}

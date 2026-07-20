package org.cloudfoundry.multiapps.controller.persistence.services.resilience;

import com.google.cloud.storage.StorageException;

public class GcpTransientErrorClassifier implements RetryableErrorClassifier {

    @Override
    public boolean isRetryable(Throwable cause) {
        if (isIoFailure(cause)) {
            return true;
        }
        if (!(cause instanceof StorageException storageException)) {
            return false;
        }
        return storageException.isRetryable() || RetryableHttpStatuses.contains(storageException.getCode());
    }

}

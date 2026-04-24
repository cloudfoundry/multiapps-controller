package org.cloudfoundry.multiapps.controller.persistence.services.resilience;

import com.azure.storage.blob.models.BlobStorageException;

public class AzureTransientErrorClassifier implements RetryableErrorClassifier {

    @Override
    public boolean isRetryable(Throwable cause) {
        if (isIoFailure(cause)) {
            return true;
        }
        return cause instanceof BlobStorageException blobStorageException
                && RetryableHttpStatuses.contains(blobStorageException.getStatusCode());
    }

}

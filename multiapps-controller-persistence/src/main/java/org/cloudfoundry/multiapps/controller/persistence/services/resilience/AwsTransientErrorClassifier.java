package org.cloudfoundry.multiapps.controller.persistence.services.resilience;

import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.exception.SdkServiceException;

public class AwsTransientErrorClassifier implements RetryableErrorClassifier {

    @Override
    public boolean isRetryable(Throwable cause) {
        if (isIoFailure(cause)) {
            return true;
        }
        if (cause instanceof ApiCallTimeoutException || cause instanceof ApiCallAttemptTimeoutException) {
            return true;
        }
        if (cause instanceof SdkServiceException sdkServiceException
                && RetryableHttpStatuses.contains(sdkServiceException.statusCode())) {
            return true;
        }
        return cause instanceof SdkException sdkException && sdkException.retryable();
    }

}

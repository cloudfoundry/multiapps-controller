package org.cloudfoundry.multiapps.controller.persistence.services.resilience;

import com.aliyun.oss.ClientException;

public class AliCloudTransientErrorClassifier implements RetryableErrorClassifier {

    @Override
    public boolean isRetryable(Throwable cause) {
        if (isIoFailure(cause)) {
            return true;
        }

        if (cause instanceof ClientException clientException) {
            return isIoFailure(clientException.getCause());
        }

        return false;
    }

}

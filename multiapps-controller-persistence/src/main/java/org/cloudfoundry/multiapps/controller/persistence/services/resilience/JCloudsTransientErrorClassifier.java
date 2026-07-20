package org.cloudfoundry.multiapps.controller.persistence.services.resilience;

import org.jclouds.http.HttpResponse;
import org.jclouds.http.HttpResponseException;

public class JCloudsTransientErrorClassifier implements RetryableErrorClassifier {

    @Override
    public boolean isRetryable(Throwable cause) {
        if (isIoFailure(cause)) {
            return true;
        }
        if (!(cause instanceof HttpResponseException httpResponseException)) {
            return false;
        }
        HttpResponse response = httpResponseException.getResponse();
        return response != null && RetryableHttpStatuses.contains(response.getStatusCode());
    }

}

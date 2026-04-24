package org.cloudfoundry.multiapps.controller.persistence.services.resilience;

/**
 * Fallback classifier used when no object store provider is bound. It treats every error as
 * non-retryable so the resilient executor performs a single attempt — there is no transient
 * cloud-store failure mode to retry against in that environment.
 */
public class NoRetryErrorClassifier implements RetryableErrorClassifier {

    @Override
    public boolean isRetryable(Throwable cause) {
        return false;
    }

}

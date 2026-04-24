package org.cloudfoundry.multiapps.controller.persistence.services.resilience;

import java.io.IOException;
import java.io.UncheckedIOException;

public interface RetryableErrorClassifier {

    boolean isRetryable(Throwable cause);

    default boolean isIoFailure(Throwable cause) {
        return cause instanceof IOException || cause instanceof UncheckedIOException;
    }

}

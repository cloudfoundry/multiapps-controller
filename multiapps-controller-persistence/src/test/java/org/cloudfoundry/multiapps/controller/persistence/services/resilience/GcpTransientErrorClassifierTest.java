package org.cloudfoundry.multiapps.controller.persistence.services.resilience;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Stream;
import com.google.cloud.storage.StorageException;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GcpTransientErrorClassifierTest {

    private final GcpTransientErrorClassifier classifier = new GcpTransientErrorClassifier();

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    void isRetryable(Throwable cause, boolean expectedRetryable) {
        assertEquals(expectedRetryable, classifier.isRetryable(cause));
    }

    static Stream<Arguments> testCases() {
        return Stream.of(Arguments.of(Named.of("IOException is retryable", new IOException("network failure")), true),
                         Arguments.of(Named.of("UncheckedIOException is retryable", new UncheckedIOException(new IOException("timeout"))),
                                      true),
                         Arguments.of(Named.of("StorageException flagged retryable with non-retryable status is retryable",
                                               mockStorageException(200, true)),
                                      true),
                         Arguments.of(Named.of("StorageException with 503 status not flagged retryable is retryable",
                                               mockStorageException(503, false)),
                                      true),
                         Arguments.of(Named.of("StorageException with 503 status and flagged retryable is retryable",
                                               mockStorageException(503, true)),
                                      true),
                         Arguments.of(Named.of("StorageException with 404 status not flagged retryable is not retryable",
                                               mockStorageException(404, false)),
                                      false),
                         Arguments.of(Named.of("StorageException with 200 status not flagged retryable is not retryable",
                                               mockStorageException(200, false)),
                                      false),
                         Arguments.of(Named.of("IllegalArgumentException is not retryable", new IllegalArgumentException("invalid key")),
                                      false));
    }

    private static StorageException mockStorageException(int code, boolean retryable) {
        StorageException exception = mock(StorageException.class);
        when(exception.getCode()).thenReturn(code);
        when(exception.isRetryable()).thenReturn(retryable);
        return exception;
    }

}

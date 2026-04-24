package org.cloudfoundry.multiapps.controller.persistence.services.resilience;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Stream;
import com.azure.storage.blob.models.BlobStorageException;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AzureTransientErrorClassifierTest {

    private final AzureTransientErrorClassifier classifier = new AzureTransientErrorClassifier();

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    void isRetryable(Throwable cause, boolean expectedRetryable) {
        assertEquals(expectedRetryable, classifier.isRetryable(cause));
    }

    static Stream<Arguments> testCases() {
        return Stream.of(Arguments.of(Named.of("IOException is retryable", new IOException("network failure")), true),
                         Arguments.of(Named.of("UncheckedIOException is retryable", new UncheckedIOException(new IOException("timeout"))),
                                      true),
                         Arguments.of(Named.of("BlobStorageException with 503 status is retryable", mockBlobStorageException(503)), true),
                         Arguments.of(Named.of("BlobStorageException with 429 status is retryable", mockBlobStorageException(429)), true),
                         Arguments.of(Named.of("BlobStorageException with 404 status is not retryable", mockBlobStorageException(404)),
                                      false),
                         Arguments.of(Named.of("BlobStorageException with 200 status is not retryable", mockBlobStorageException(200)),
                                      false),
                         Arguments.of(Named.of("IllegalArgumentException is not retryable", new IllegalArgumentException("invalid key")),
                                      false),
                         Arguments.of(Named.of("RuntimeException is not retryable", new RuntimeException("unrelated")), false));
    }

    private static BlobStorageException mockBlobStorageException(int statusCode) {
        BlobStorageException exception = mock(BlobStorageException.class);
        when(exception.getStatusCode())
            .thenReturn(statusCode);
        return exception;
    }

}

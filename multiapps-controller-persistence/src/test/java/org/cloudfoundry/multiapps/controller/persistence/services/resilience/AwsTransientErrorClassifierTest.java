package org.cloudfoundry.multiapps.controller.persistence.services.resilience;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Stream;
import org.cloudfoundry.multiapps.common.SLException;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.exception.ApiCallAttemptTimeoutException;
import software.amazon.awssdk.core.exception.ApiCallTimeoutException;
import software.amazon.awssdk.core.exception.RetryableException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AwsTransientErrorClassifierTest {

    private final AwsTransientErrorClassifier classifier = new AwsTransientErrorClassifier();

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    void isRetryable(Throwable cause, boolean expectedRetryable) {
        assertEquals(expectedRetryable, classifier.isRetryable(cause));
    }

    static Stream<Arguments> testCases() {
        return Stream.of(Arguments.of(Named.of("IOException is retryable", new IOException("network failure")), true),
                         Arguments.of(Named.of("UncheckedIOException is retryable", new UncheckedIOException(new IOException("timeout"))),
                                      true),
                         Arguments.of(Named.of("ApiCallTimeoutException is retryable",
                                               ApiCallTimeoutException.builder()
                                                                      .message("api call timeout")
                                                                      .build()),
                                      true),
                         Arguments.of(Named.of("ApiCallAttemptTimeoutException is retryable",
                                               ApiCallAttemptTimeoutException.builder()
                                                                             .message("attempt timeout")
                                                                             .build()),
                                      true),
                         Arguments.of(Named.of("S3Exception with 503 status is retryable",
                                               S3Exception.builder()
                                                          .message("service unavailable")
                                                          .statusCode(503)
                                                          .build()),
                                      true),
                         Arguments.of(Named.of("S3Exception with 404 status is not retryable",
                                               S3Exception.builder()
                                                          .message("not found")
                                                          .statusCode(404)
                                                          .build()),
                                      false),
                         Arguments.of(Named.of("RetryableException is retryable",
                                               RetryableException.builder()
                                                                 .message("retryable error")
                                                                 .build()),
                                      true),
                         Arguments.of(Named.of("Bare SdkException is not retryable",
                                               SdkException.builder()
                                                           .message("unspecified failure")
                                                           .build()),
                                      false),
                         Arguments.of(Named.of("IllegalArgumentException is not retryable", new IllegalArgumentException("invalid key")),
                                      false),
                         Arguments.of(Named.of("SLException is not retryable", new SLException("Validation error")), false));
    }

}

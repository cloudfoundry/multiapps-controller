package org.cloudfoundry.multiapps.controller.persistence.services.resilience;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.stream.Stream;
import org.jclouds.http.HttpResponse;
import org.jclouds.http.HttpResponseException;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JCloudsTransientErrorClassifierTest {

    private final JCloudsTransientErrorClassifier classifier = new JCloudsTransientErrorClassifier();

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    void isRetryable(Throwable cause, boolean expectedRetryable) {
        assertEquals(expectedRetryable, classifier.isRetryable(cause));
    }

    static Stream<Arguments> testCases() {
        return Stream.of(Arguments.of(Named.of("IOException is retryable", new IOException("network failure")), true),
                         Arguments.of(Named.of("UncheckedIOException is retryable", new UncheckedIOException(new IOException("timeout"))),
                                      true),
                         Arguments.of(Named.of("HttpResponseException with 503 status is retryable", mockHttpResponseException(503)),
                                      true),
                         Arguments.of(Named.of("HttpResponseException with 429 status is retryable", mockHttpResponseException(429)),
                                      true),
                         Arguments.of(Named.of("HttpResponseException with 404 status is not retryable", mockHttpResponseException(404)),
                                      false),
                         Arguments.of(Named.of("HttpResponseException with 200 status is not retryable", mockHttpResponseException(200)),
                                      false),
                         Arguments.of(
                             Named.of("HttpResponseException with null response is not retryable", mockHttpResponseException(null)),
                             false),
                         Arguments.of(Named.of("IllegalArgumentException is not retryable", new IllegalArgumentException("invalid key")),
                                      false));
    }

    private static HttpResponseException mockHttpResponseException(Integer statusCode) {
        HttpResponseException exception = mock(HttpResponseException.class);
        if (statusCode == null) {
            when(exception.getResponse())
                .thenReturn(null);
        } else {
            HttpResponse response = mock(HttpResponse.class);
            when(response.getStatusCode())
                .thenReturn(statusCode);
            when(exception.getResponse())
                .thenReturn(response);
        }
        return exception;
    }

}

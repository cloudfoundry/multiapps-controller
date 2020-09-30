package org.cloudfoundry.multiapps.controller.core.cf.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.stream.Stream;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.controller.client.util.ResilientCloudOperationExecutor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

class CustomControllerClientErrorHandlerTest {

    private static final ResilientCloudOperationExecutor NULL_RETRIER = new ResilientCloudOperationExecutor().withRetryCount(0)
                                                                                                             .withWaitTimeBetweenRetriesInMillis(0);

    public static Stream<Arguments> testHandleErrorsWithRightExceptionType() {
        return Stream.of(
// @formatter:off
                // (0) The response body contains a description in the supported format:
                Arguments.of(prepareHttpStatusCodeException(HttpStatus.BAD_GATEWAY, "Service broker error", "cf-error-response-body-0.json"),
                        new CloudOperationException(HttpStatus.BAD_GATEWAY, "Service broker error", "Application currency-services-core-uaa-dev1!i211 does not exist")),
                // (1) The response body does not contain a description (but does contain other information in a JSON format):
                Arguments.of(prepareHttpStatusCodeException(HttpStatus.BAD_GATEWAY, "Service broker error", "cf-error-response-body-1.json"),
                        new CloudOperationException(HttpStatus.BAD_GATEWAY, "Service broker error", null)),
                // (2) The response body contains a description in an unsupported format:
                Arguments.of(prepareHttpStatusCodeException(HttpStatus.BAD_GATEWAY, "Service broker error", "cf-error-response-body-2.json"),
                        new CloudOperationException(HttpStatus.BAD_GATEWAY, "Service broker error", null)),
                // (3) The response body contains a description in an unsupported format:
                Arguments.of(prepareHttpStatusCodeException(HttpStatus.BAD_GATEWAY, "Service broker error", "cf-error-response-body-3.json"),
                        new CloudOperationException(HttpStatus.BAD_GATEWAY, "Service broker error", null))
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testHandleErrorsWithRightExceptionType(HttpStatusCodeException exceptionToThrow, CloudOperationException expected) {
        try {
            new CustomControllerClientErrorHandler().withExecutorFactory(() -> NULL_RETRIER)
                                                    .handleErrors(() -> {
                                                        throw exceptionToThrow;
                                                    });
        } catch (CloudOperationException result) {
            assertEquals(expected.getStatusCode(), result.getStatusCode());
            assertEquals(expected.getStatusText(), result.getStatusText());
            assertEquals(expected.getDescription(), result.getDescription());
            return;
        }
        fail();
    }

    private static HttpStatusCodeException prepareHttpStatusCodeException(HttpStatus statusCode, String statusText,
                                                                          String locationOfFileContainingResponseBody) {
        HttpStatusCodeException exception = Mockito.mock(HttpStatusCodeException.class);
        Mockito.when(exception.getStatusCode())
               .thenReturn(statusCode);
        Mockito.when(exception.getStatusText())
               .thenReturn(statusText);
        String responseBody = TestUtil.getResourceAsString(locationOfFileContainingResponseBody,
                                                           CustomControllerClientErrorHandlerTest.class);
        Mockito.when(exception.getResponseBodyAsString())
               .thenReturn(responseBody);
        return exception;
    }

    @Test
    void testHandleErrorsWithWrongExceptionType() {
        ResilientCloudOperationExecutor resilientCloudOperationExecutor = new ResilientCloudOperationExecutor().withWaitTimeBetweenRetriesInMillis(0);
        CustomControllerClientErrorHandler customControllerClientErrorHandler = new CustomControllerClientErrorHandler().withExecutorFactory(() -> resilientCloudOperationExecutor);
        Assertions.assertThrows(IllegalArgumentException.class, () -> customControllerClientErrorHandler.handleErrors(() -> {
            throw new IllegalArgumentException("Should not be handled by the error handler");
        }));
    }

}

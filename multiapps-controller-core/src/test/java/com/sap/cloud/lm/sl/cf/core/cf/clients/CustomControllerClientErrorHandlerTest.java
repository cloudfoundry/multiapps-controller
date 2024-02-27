package com.sap.cloud.lm.sl.cf.core.cf.clients;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

import com.sap.cloud.lm.sl.cf.client.util.ExecutionRetrier;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Enclosed.class)
public class CustomControllerClientErrorHandlerTest {

    private static final ExecutionRetrier NULL_RETRIER = new ExecutionRetrier().withRetryCount(0)
                                                                               .withWaitTimeBetweenRetriesInMillis(0);

    public static class StandardTest {

        @Test(expected = IllegalArgumentException.class)
        public void testHandleErrorsWithWrongExceptionType() {
            new CustomControllerClientErrorHandler().handleErrors(() -> {
                throw new IllegalArgumentException("Should not be handled by the error handler");
            });
        }

    }

    @RunWith(Parameterized.class)
    public static class ParameterizedTest {

        private HttpStatusCodeException exceptionToThrow;
        private CloudOperationException expected;
        public ParameterizedTest(HttpStatusCodeException exceptionToThrow, CloudOperationException expected) {
            this.exceptionToThrow = exceptionToThrow;
            this.expected = expected;
        }

        @Parameters
        public static Iterable<Object[]> getParameters() throws IOException {
            return Arrays.asList(new Object[][] {
// @formatter:off
                // (0) The response body contains a description in the supported format:
                {
                    prepareHttpStatusCodeException(HttpStatus.BAD_GATEWAY, "Service broker error", "cf-error-response-body-0.json"),
                         new CloudOperationException(HttpStatus.BAD_GATEWAY, "Service broker error", "Application currency-services-core-uaa-dev1!i211 does not exist"),
                },
                // (1) The response body does not contain a description (but does contain other information in a JSON format):
                {
                    prepareHttpStatusCodeException(HttpStatus.BAD_GATEWAY, "Service broker error", "cf-error-response-body-1.json"),
                         new CloudOperationException(HttpStatus.BAD_GATEWAY, "Service broker error", null),
                },
                // (2) The response body contains a description in an unsupported format:
                {
                    prepareHttpStatusCodeException(HttpStatus.BAD_GATEWAY, "Service broker error", "cf-error-response-body-2.json"),
                         new CloudOperationException(HttpStatus.BAD_GATEWAY, "Service broker error", null),
                },
                // (3) The response body contains a description in an unsupported format:
                {
                    prepareHttpStatusCodeException(HttpStatus.BAD_GATEWAY, "Service broker error", "cf-error-response-body-3.json"),
                         new CloudOperationException(HttpStatus.BAD_GATEWAY, "Service broker error", null),
                },
// @formatter:on
            });
        }

        private static HttpStatusCodeException prepareHttpStatusCodeException(HttpStatus statusCode, String statusText,
                                                                              String locationOfFileContainingResponseBody)
            throws IOException {
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
        public void testHandleErrorsWithRightExceptionType() {
            try {
                new CustomControllerClientErrorHandler(NULL_RETRIER).handleErrors(() -> {
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

    }

}

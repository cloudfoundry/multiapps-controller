package org.cloudfoundry.multiapps.controller.core.http;

import java.io.IOException;
import java.util.function.Function;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.cloudfoundry.multiapps.common.SLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TimeoutHttpExecutorTest {

    private HttpClient mockHttpClient;
    private HttpUriRequest mockRequest;
    private HttpResponse mockResponse;
    private TimeoutHttpExecutor timeoutHttpExecutor;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        mockRequest = mock(HttpUriRequest.class);
        mockResponse = mock(HttpResponse.class);
        timeoutHttpExecutor = new TimeoutHttpExecutor(mockHttpClient);
    }

    @Test
    void testExecuteWithTimeoutSuccessfulExecution() throws IOException {
        when(mockHttpClient.execute(mockRequest)).thenReturn(mockResponse);
        Function<HttpResponse, String> responseHandler = response -> "Success";

        String result = timeoutHttpExecutor.executeWithTimeout(mockRequest, 1000, responseHandler);

        assertEquals("Success", result);
        verify(mockHttpClient, times(1)).execute(mockRequest);
        verify(mockRequest, never()).abort();
    }

    @Test
    void testExecuteWithTimeoutRequestAbortedDueToTimeout() throws IOException {
        when(mockHttpClient.execute(mockRequest)).thenAnswer(invocation -> {
            Thread.sleep(200); // Simulate a delay
            return mockResponse;
        });
        Function<HttpResponse, String> responseHandler = response -> "Success";

        timeoutHttpExecutor.executeWithTimeout(mockRequest, 100, responseHandler);
        verify(mockRequest, times(1)).abort();
    }

    @Test
    void testExecuteWithTimeoutIOExceptionThrown() throws IOException {
        when(mockHttpClient.execute(mockRequest)).thenThrow(new IOException("Connection error"));
        Function<HttpResponse, String> responseHandler = response -> "Success";

        SLException exception = assertThrows(SLException.class,
                                             () -> timeoutHttpExecutor.executeWithTimeout(mockRequest, 1000, responseHandler));
        assertEquals("Connection error", exception.getMessage());
        verify(mockRequest, never()).abort();
    }

}

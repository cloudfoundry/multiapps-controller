package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CloudControllerResponseErrorHandlerTest {

    private final CloudControllerResponseErrorHandler handler = new CloudControllerResponseErrorHandler();

    @Test
    void testWithV2Error() {
        HttpStatus statusCode = HttpStatus.UNPROCESSABLE_ENTITY;
        ClientHttpResponseMock response = new ClientHttpResponseMock(statusCode, getClass().getResourceAsStream("v2-error.json"));
        CloudOperationException expectedException = new CloudOperationException(statusCode,
                                                                                statusCode.getReasonPhrase(),
                                                                                "Request invalid due to parse error: Field: name, Error: Missing field name, Field: space_guid, Error: Missing field space_guid, Field: service_plan_guid, Error: Missing field service_plan_guid");
        testWithError(response, expectedException);
    }

    @Test
    void testWithV3Error() {
        HttpStatus statusCode = HttpStatus.BAD_REQUEST;
        ClientHttpResponseMock response = new ClientHttpResponseMock(statusCode, getClass().getResourceAsStream("v3-error.json"));
        CloudOperationException expectedException = new CloudOperationException(statusCode,
                                                                                statusCode.getReasonPhrase(),
                                                                                "memory_in_mb exceeds organization memory quota\ndisk_in_mb exceeds organization disk quota");
        testWithError(response, expectedException);
    }

    @Test
    void testWithInvalidError() {
        HttpStatus statusCode = HttpStatus.BAD_REQUEST;
        ClientHttpResponseMock response = new ClientHttpResponseMock(statusCode, toInputStream("blabla"));
        CloudOperationException expectedException = new CloudOperationException(statusCode, statusCode.getReasonPhrase());
        testWithError(response, expectedException);
    }

    @Test
    void testWithEmptyResponse() {
        HttpStatus statusCode = HttpStatus.BAD_REQUEST;
        ClientHttpResponseMock response = new ClientHttpResponseMock(statusCode, toInputStream("{    }"));
        CloudOperationException expectedException = new CloudOperationException(statusCode, statusCode.getReasonPhrase());
        testWithError(response, expectedException);
    }

    @Test
    void testWith429AndRetryAfterHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.RETRY_AFTER, "60");
        ClientHttpResponseMock response = new ClientHttpResponseMock(HttpStatus.TOO_MANY_REQUESTS, toInputStream("{}"), headers);
        CloudOperationException e = assertThrows(CloudOperationException.class, () -> handler.handleError(response));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatusCode());
        assertEquals(60L, e.getRetryAfterSeconds());
    }

    @Test
    void testWith429WithoutRetryAfterHeader() {
        ClientHttpResponseMock response = new ClientHttpResponseMock(HttpStatus.TOO_MANY_REQUESTS, toInputStream("{}"));
        CloudOperationException e = assertThrows(CloudOperationException.class, () -> handler.handleError(response));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatusCode());
        assertNull(e.getRetryAfterSeconds());
    }

    @Test
    void testWith429AndNonNumericRetryAfterHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.RETRY_AFTER, "not-a-number");
        ClientHttpResponseMock response = new ClientHttpResponseMock(HttpStatus.TOO_MANY_REQUESTS, toInputStream("{}"), headers);
        CloudOperationException e = assertThrows(CloudOperationException.class, () -> handler.handleError(response));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatusCode());
        assertNull(e.getRetryAfterSeconds());
    }

    @Test
    void testNon429DoesNotSetRetryAfterSeconds() {
        ClientHttpResponseMock response = new ClientHttpResponseMock(HttpStatus.INTERNAL_SERVER_ERROR, toInputStream("{}"));
        CloudOperationException e = assertThrows(CloudOperationException.class, () -> handler.handleError(response));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
        assertNull(e.getRetryAfterSeconds());
    }

    @Test
    void testWith429RetainsRetryAfterAndDescriptionWhenBodyPresent() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.RETRY_AFTER, "30");
        ClientHttpResponseMock response = new ClientHttpResponseMock(HttpStatus.TOO_MANY_REQUESTS,
                                                                     toInputStream("{\"description\":\"rate limit exceeded\"}"), headers);
        CloudOperationException e = assertThrows(CloudOperationException.class, () -> handler.handleError(response));
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatusCode());
        assertEquals(30L, e.getRetryAfterSeconds());
        assertEquals("rate limit exceeded", e.getDescription());
    }

    private void testWithError(ClientHttpResponseMock response, CloudOperationException expectedException) {
        CloudOperationException exception = assertThrows(CloudOperationException.class, () -> handler.handleError(response));
        assertEquals(expectedException.getStatusCode(), exception.getStatusCode());
        assertEquals(expectedException.getStatusText(), exception.getStatusText());
        assertEquals(expectedException.getDescription(), exception.getDescription());
    }

    @Test
    void testWithNonClientOrServerError() {
        HttpStatus statusCode = HttpStatus.PERMANENT_REDIRECT;
        ClientHttpResponseMock response = new ClientHttpResponseMock(HttpStatus.PERMANENT_REDIRECT, null);
        RestClientException exception = assertThrows(RestClientException.class, () -> handler.handleError(response));
        assertEquals("Unknown status code [" + statusCode + "]", exception.getMessage());
    }

    private InputStream toInputStream(String string) {
        return new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
    }

    private static class ClientHttpResponseMock implements ClientHttpResponse {

        private final HttpStatus statusCode;
        private final InputStream body;
        private final HttpHeaders headers;

        public ClientHttpResponseMock(HttpStatus statusCode, InputStream body) {
            this(statusCode, body, new HttpHeaders());
        }

        public ClientHttpResponseMock(HttpStatus statusCode, InputStream body, HttpHeaders headers) {
            this.statusCode = statusCode;
            this.body = body;
            this.headers = headers;
        }

        @Override
        public InputStream getBody() {
            return body;
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }

        @Override
        public HttpStatus getStatusCode() {
            return statusCode;
        }

        @Override
        public int getRawStatusCode() {
            return statusCode.value();
        }

        @Override
        public String getStatusText() {
            return statusCode.getReasonPhrase();
        }

        @Override
        public void close() {

        }

    }

}


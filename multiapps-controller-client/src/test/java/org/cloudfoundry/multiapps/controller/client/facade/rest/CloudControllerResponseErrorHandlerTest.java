package org.cloudfoundry.multiapps.controller.client.facade.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestClientException;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;

class CloudControllerResponseErrorHandlerTest {

    private final CloudControllerResponseErrorHandler handler = new CloudControllerResponseErrorHandler();

    @Test
    void testWithV2Error() throws IOException {
        HttpStatus statusCode = HttpStatus.UNPROCESSABLE_ENTITY;
        ClientHttpResponseMock response = new ClientHttpResponseMock(statusCode, getClass().getResourceAsStream("v2-error.json"));
        CloudOperationException expectedException = new CloudOperationException(statusCode,
                                                                                statusCode.getReasonPhrase(),
                                                                                "Request invalid due to parse error: Field: name, Error: Missing field name, Field: space_guid, Error: Missing field space_guid, Field: service_plan_guid, Error: Missing field service_plan_guid");
        testWithError(response, expectedException);
    }

    @Test
    void testWithV3Error() throws IOException {
        HttpStatus statusCode = HttpStatus.BAD_REQUEST;
        ClientHttpResponseMock response = new ClientHttpResponseMock(statusCode, getClass().getResourceAsStream("v3-error.json"));
        CloudOperationException expectedException = new CloudOperationException(statusCode,
                                                                                statusCode.getReasonPhrase(),
                                                                                "memory_in_mb exceeds organization memory quota\ndisk_in_mb exceeds organization disk quota");
        testWithError(response, expectedException);
    }

    @Test
    void testWithInvalidError() throws IOException {
        HttpStatus statusCode = HttpStatus.BAD_REQUEST;
        ClientHttpResponseMock response = new ClientHttpResponseMock(statusCode, toInputStream("blabla"));
        CloudOperationException expectedException = new CloudOperationException(statusCode, statusCode.getReasonPhrase());
        testWithError(response, expectedException);
    }

    @Test
    void testWithEmptyResponse() throws IOException {
        HttpStatus statusCode = HttpStatus.BAD_REQUEST;
        ClientHttpResponseMock response = new ClientHttpResponseMock(statusCode, toInputStream("{    }"));
        CloudOperationException expectedException = new CloudOperationException(statusCode, statusCode.getReasonPhrase());
        testWithError(response, expectedException);
    }

    @Test
    void testWith429AndRetryAfterHeader() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.RETRY_AFTER, "60");
        ClientHttpResponseMock response = new ClientHttpResponseMock(HttpStatus.TOO_MANY_REQUESTS, toInputStream("{}"), headers);
        try {
            handler.handleError(response);
            fail("Expected an exception");
        } catch (CloudOperationException e) {
            assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatusCode());
            assertEquals(60L, e.getRetryAfterSeconds());
        }
    }

    @Test
    void testWith429WithoutRetryAfterHeader() throws IOException {
        ClientHttpResponseMock response = new ClientHttpResponseMock(HttpStatus.TOO_MANY_REQUESTS, toInputStream("{}"));
        try {
            handler.handleError(response);
            fail("Expected an exception");
        } catch (CloudOperationException e) {
            assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatusCode());
            assertNull(e.getRetryAfterSeconds());
        }
    }

    @Test
    void testWith429AndNonNumericRetryAfterHeader() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.RETRY_AFTER, "not-a-number");
        ClientHttpResponseMock response = new ClientHttpResponseMock(HttpStatus.TOO_MANY_REQUESTS, toInputStream("{}"), headers);
        try {
            handler.handleError(response);
            fail("Expected an exception");
        } catch (CloudOperationException e) {
            assertEquals(HttpStatus.TOO_MANY_REQUESTS, e.getStatusCode());
            assertNull(e.getRetryAfterSeconds());
        }
    }

    @Test
    void testNon429DoesNotSetRetryAfterSeconds() throws IOException {
        ClientHttpResponseMock response = new ClientHttpResponseMock(HttpStatus.INTERNAL_SERVER_ERROR, toInputStream("{}"));
        try {
            handler.handleError(response);
            fail("Expected an exception");
        } catch (CloudOperationException e) {
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
            assertNull(e.getRetryAfterSeconds());
        }
    }

    private void testWithError(ClientHttpResponseMock response, CloudOperationException expectedException) throws IOException {
        try {
            handler.handleError(response);
            fail("Expected an exception");
        } catch (CloudOperationException exception) {
            assertEquals(expectedException.getStatusCode(), exception.getStatusCode());
            assertEquals(expectedException.getStatusText(), exception.getStatusText());
            assertEquals(expectedException.getDescription(), exception.getDescription());
        }
    }

    @Test
    void testWithNonClientOrServerError() throws IOException {
        HttpStatus statusCode = HttpStatus.PERMANENT_REDIRECT;
        ClientHttpResponseMock response = new ClientHttpResponseMock(HttpStatus.PERMANENT_REDIRECT, null);
        try {
            handler.handleError(response);
            fail("Expected an exception");
        } catch (RestClientException exception) {
            assertEquals("Unknown status code [" + statusCode + "]", exception.getMessage());
        }
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


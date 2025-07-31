package org.cloudfoundry.multiapps.controller.client.facade.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        public ClientHttpResponseMock(HttpStatus statusCode, InputStream body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override
        public InputStream getBody() {
            return body;
        }

        @Override
        public HttpHeaders getHeaders() {
            throw new UnsupportedOperationException();
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

package org.cloudfoundry.multiapps.controller.client.facade;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CloudControllerExceptionTest {

    @Test
    void testStatusOnlyConstructorDecoratesMessage() {
        CloudControllerException e = new CloudControllerException(HttpStatus.NOT_FOUND);

        Assertions.assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        Assertions.assertEquals("Controller operation failed: 404 Not Found", e.getMessage());
    }

    @Test
    void testStatusAndStatusTextConstructor() {
        CloudControllerException e = new CloudControllerException(HttpStatus.BAD_REQUEST, "Bad Request");

        Assertions.assertEquals("Bad Request", e.getStatusText());
        Assertions.assertEquals("Controller operation failed: 400 Bad Request", e.getMessage());
    }

    @Test
    void testStatusStatusTextDescriptionConstructor() {
        CloudControllerException e = new CloudControllerException(HttpStatus.UNAUTHORIZED, "Unauthorized", "expired");

        Assertions.assertEquals("expired", e.getDescription());
        Assertions.assertEquals("Controller operation failed: 401 Unauthorized: expired", e.getMessage());
    }

    @Test
    void testWrappingConstructorPreservesFieldsAndCause() {
        CloudOperationException source = new CloudOperationException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "boom");

        CloudControllerException e = new CloudControllerException(source);

        Assertions.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
        Assertions.assertEquals("Internal Server Error", e.getStatusText());
        Assertions.assertEquals("boom", e.getDescription());
        Assertions.assertSame(source, e.getCause());
        Assertions.assertEquals("Controller operation failed: 500 Internal Server Error: boom", e.getMessage());
    }
}

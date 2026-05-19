package org.cloudfoundry.multiapps.controller.client.facade;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CloudServiceBrokerExceptionTest {

    @Test
    void testStatusOnlyConstructorDecoratesMessage() {
        CloudServiceBrokerException e = new CloudServiceBrokerException(HttpStatus.NOT_FOUND);

        Assertions.assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        Assertions.assertEquals("Service broker operation failed: 404 Not Found", e.getMessage());
    }

    @Test
    void testStatusAndStatusTextConstructor() {
        CloudServiceBrokerException e = new CloudServiceBrokerException(HttpStatus.BAD_REQUEST, "Bad Request");

        Assertions.assertEquals("Service broker operation failed: 400 Bad Request", e.getMessage());
    }

    @Test
    void testStatusStatusTextDescriptionConstructor() {
        CloudServiceBrokerException e = new CloudServiceBrokerException(HttpStatus.CONFLICT, "Conflict", "already exists");

        Assertions.assertEquals("Service broker operation failed: 409 Conflict: already exists", e.getMessage());
    }

    @Test
    void testWrappingConstructorPreservesFieldsAndCause() {
        CloudOperationException source = new CloudOperationException(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", "broker rejected");

        CloudServiceBrokerException e = new CloudServiceBrokerException(source);

        Assertions.assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, e.getStatusCode());
        Assertions.assertEquals("broker rejected", e.getDescription());
        Assertions.assertSame(source, e.getCause());
        Assertions.assertEquals("Service broker operation failed: 422 Unprocessable Entity: broker rejected", e.getMessage());
    }
}

package org.cloudfoundry.multiapps.controller.client.facade;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CloudExceptionTest {

    @Test
    void testCauseOnlyConstructor() {
        Throwable cause = new RuntimeException("inner");

        CloudException e = new CloudException(cause);

        Assertions.assertSame(cause, e.getCause());
    }

    @Test
    void testMessageAndCauseConstructor() {
        Throwable cause = new RuntimeException("inner");

        CloudException e = new CloudException("outer", cause);

        Assertions.assertEquals("outer", e.getMessage());
        Assertions.assertSame(cause, e.getCause());
    }

    @Test
    void testMessageOnlyConstructor() {
        CloudException e = new CloudException("only-message");

        Assertions.assertEquals("only-message", e.getMessage());
        Assertions.assertNull(e.getCause());
    }
}

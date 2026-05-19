package org.cloudfoundry.multiapps.controller.persistence.services;

import org.cloudfoundry.multiapps.common.SLException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OperationLogStorageExceptionTest {

    @Test
    void testIsSlException() {
        Assertions.assertTrue(SLException.class.isAssignableFrom(OperationLogStorageException.class));
    }

    @Test
    void testCauseConstructor() {
        Throwable cause = new RuntimeException("boom");

        OperationLogStorageException e = new OperationLogStorageException(cause);

        Assertions.assertSame(cause, e.getCause());
    }

    @Test
    void testMessageConstructor() {
        OperationLogStorageException e = new OperationLogStorageException("storage failed");

        Assertions.assertEquals("storage failed", e.getMessage());
        Assertions.assertNull(e.getCause());
    }

    @Test
    void testMessageAndCauseConstructorPreservesMessage() {
        // The (String, Throwable) ctor resolves to SLException(String, Object...),
        // so the message survives but the Throwable is treated as a MessageFormat argument.
        Throwable cause = new RuntimeException("boom");

        OperationLogStorageException e = new OperationLogStorageException("storage failed", cause);

        Assertions.assertEquals("storage failed", e.getMessage());
    }
}

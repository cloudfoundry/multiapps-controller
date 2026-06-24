package org.cloudfoundry.multiapps.controller.persistence.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class FileStorageExceptionTest {

    @Test
    void testCauseConstructor() {
        Throwable cause = new RuntimeException("boom");

        FileStorageException e = new FileStorageException(cause);

        Assertions.assertSame(cause, e.getCause());
    }

    @Test
    void testMessageConstructor() {
        FileStorageException e = new FileStorageException("disk full");

        Assertions.assertEquals("disk full", e.getMessage());
        Assertions.assertNull(e.getCause());
    }

    @Test
    void testMessageAndCauseConstructor() {
        Throwable cause = new RuntimeException("boom");

        FileStorageException e = new FileStorageException("disk full", cause);

        Assertions.assertEquals("disk full", e.getMessage());
        Assertions.assertSame(cause, e.getCause());
    }
}

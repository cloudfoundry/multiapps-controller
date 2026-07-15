package org.cloudfoundry.multiapps.controller.client.facade;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CloudOperationExceptionTest {

    @Test
    void testStatusOnlyConstructorUsesReasonPhraseAsStatusText() {
        CloudOperationException e = new CloudOperationException(HttpStatus.NOT_FOUND);

        Assertions.assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        Assertions.assertEquals(HttpStatus.NOT_FOUND.getReasonPhrase(), e.getStatusText());
        Assertions.assertNull(e.getDescription());
        Assertions.assertEquals("404 Not Found", e.getMessage());
    }

    @Test
    void testStatusAndStatusTextConstructor() {
        CloudOperationException e = new CloudOperationException(HttpStatus.BAD_REQUEST, "Bad Request");

        Assertions.assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        Assertions.assertEquals("Bad Request", e.getStatusText());
        Assertions.assertNull(e.getDescription());
        Assertions.assertEquals("400 Bad Request", e.getMessage());
    }

    @Test
    void testDescriptionConstructorIncludesDescriptionInMessage() {
        CloudOperationException e = new CloudOperationException(HttpStatus.UNAUTHORIZED, "Unauthorized", "token expired");

        Assertions.assertEquals("token expired", e.getDescription());
        Assertions.assertEquals("401 Unauthorized: token expired", e.getMessage());
    }

    @Test
    void testCauseIsPropagatedToSuper() {
        Throwable cause = new RuntimeException("boom");

        CloudOperationException e = new CloudOperationException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "x", cause);

        Assertions.assertSame(cause, e.getCause());
    }

    @Test
    void testRetryAfterSecondsNullByDefault() {
        CloudOperationException e = new CloudOperationException(HttpStatus.TOO_MANY_REQUESTS);

        Assertions.assertNull(e.getRetryAfterSeconds());
    }

    @Test
    void testRetryAfterSecondsStoredWhenProvided() {
        CloudOperationException e = new CloudOperationException(HttpStatus.TOO_MANY_REQUESTS,
                                                                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                                                                null, null, 60L);

        Assertions.assertEquals(60L, e.getRetryAfterSeconds());
    }
}


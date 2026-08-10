package org.cloudfoundry.multiapps.controller.persistence.monitoring;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import com.google.cloud.storage.StorageException;
import io.netty.handler.timeout.ReadTimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UploadTimeoutMatcherTest {

    static Stream<Arguments> testIsUploadTimeoutException() {
        return Stream.of(
            Arguments.of(new RuntimeException(new SocketTimeoutException("read timed out")), true),
            Arguments.of(new RuntimeException(new TimeoutException("operation timed out")), true),
            Arguments.of(new RuntimeException(ReadTimeoutException.INSTANCE), true),
            Arguments.of(new RuntimeException(new StorageException(504, "gateway timeout")), true),
            Arguments.of(new RuntimeException(new StorageException(500, "internal error")), false),
            Arguments.of(new RuntimeException(new StorageException(429, "too many requests")), false),
            Arguments.of(new RuntimeException(new IllegalStateException("unrelated")), false),
            Arguments.of(null, false));
    }

    @ParameterizedTest
    @MethodSource
    void testIsUploadTimeoutException(Throwable throwable, boolean expected) {
        assertEquals(expected, UploadTimeoutMatcher.isUploadTimeoutException(throwable));
    }

    @Test
    void testDeeplyNestedTimeoutCauseIsDetected() {
        Throwable deepCause = new SocketTimeoutException("read timed out");
        Throwable throwable = new RuntimeException("layer 1",
                                                   new IllegalStateException("layer 2", new RuntimeException("layer 3", deepCause)));

        assertTrue(UploadTimeoutMatcher.isUploadTimeoutException(throwable));
    }

    @Test
    void testTopLevelTimeoutWithNullCauseIsNotDetected() {
        Throwable throwable = new SocketTimeoutException("read timed out");

        assertFalse(UploadTimeoutMatcher.isUploadTimeoutException(throwable));
    }

    @Test
    void testThrowableWithNoCauseIsNotDetected() {
        assertFalse(UploadTimeoutMatcher.isUploadTimeoutException(new RuntimeException("no cause")));
    }

}

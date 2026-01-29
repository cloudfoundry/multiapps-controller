package org.cloudfoundry.multiapps.controller.shutdown.client.util;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.dto.ImmutableApplicationShutdown;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShutdownUtilTest {

    private static final int TIMEOUT_IN_SECONDS = 300;

    @Test
    void testAreThereUnstoppedInstancesWithUpstoppedInstances() {
        List<ApplicationShutdown> applicationShutdownInstances = List.of(createApplicationShutdownInstance(true, LocalDateTime.now()),
                                                                         createApplicationShutdownInstance(false, LocalDateTime.now()),
                                                                         createApplicationShutdownInstance(true, LocalDateTime.now()));

        assertTrue(ShutdownUtil.areThereUnstoppedInstances(applicationShutdownInstances));
    }

    @Test
    void testAreThereUnstoppedInstancesWithAllStoppedInstances() {
        List<ApplicationShutdown> applicationShutdownInstances = List.of(createApplicationShutdownInstance(true, LocalDateTime.now()),
                                                                         createApplicationShutdownInstance(true, LocalDateTime.now()),
                                                                         createApplicationShutdownInstance(true, LocalDateTime.now()));

        assertFalse(ShutdownUtil.areThereUnstoppedInstances(applicationShutdownInstances));
    }

    @Test
    void testIsTimeoutExceededWithTimeOutExceeded() {
        LocalDateTime timeBeforeTenMinutes = LocalDateTime.now()
                                                          .minusSeconds(ShutdownUtil.TIMEOUT_IN_SECONDS);
        ApplicationShutdown applicationShutdownInstance = createApplicationShutdownInstance(true, timeBeforeTenMinutes);

        assertTrue(ShutdownUtil.isTimeoutExceeded(applicationShutdownInstance));
    }

    @Test
    void testIsTimeoutExceededWithTimeOutNotExceeded() {
        LocalDateTime timeBeforeTenMinutes = LocalDateTime.now()
                                                          .minusSeconds(TIMEOUT_IN_SECONDS);
        ApplicationShutdown applicationShutdownInstance = createApplicationShutdownInstance(true, timeBeforeTenMinutes);

        assertFalse(ShutdownUtil.isTimeoutExceeded(applicationShutdownInstance));
    }

    private ApplicationShutdown createApplicationShutdownInstance(boolean isInstanceStopped, LocalDateTime startedAt) {
        return ImmutableApplicationShutdown.builder()
                                           .id(UUID.randomUUID()
                                                   .toString())
                                           .applicationId(UUID.randomUUID()
                                                              .toString())
                                           .applicationInstanceIndex(0)
                                           .startedAt(startedAt)
                                           .status(
                                               isInstanceStopped ? ApplicationShutdown.Status.FINISHED : ApplicationShutdown.Status.RUNNING)
                                           .build();
    }
}

package org.cloudfoundry.multiapps.controller.shutdown.client.util;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.dto.ImmutableApplicationShutdown;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShutdownUtilTest {

    public static final int TIMEOUT_IN_SECONDS = 300; //5 minutes

    @Test
    void testAreThereUnstoppedInstancesWithUpstoppedInstances() {
        List<ApplicationShutdown> applicationShutdownInstances = List.of(createApplicationShutdownInstance(true, Date.from(Instant.now())),
                                                                         createApplicationShutdownInstance(false, Date.from(Instant.now())),
                                                                         createApplicationShutdownInstance(true, Date.from(Instant.now())));

        assertTrue(ShutdownUtil.areThereUnstoppedInstances(applicationShutdownInstances));
    }

    @Test
    void testAreThereUnstoppedInstancesWithAllStoppedInstances() {
        List<ApplicationShutdown> applicationShutdownInstances = List.of(createApplicationShutdownInstance(true, Date.from(Instant.now())),
                                                                         createApplicationShutdownInstance(true, Date.from(Instant.now())),
                                                                         createApplicationShutdownInstance(true, Date.from(Instant.now())));

        assertFalse(ShutdownUtil.areThereUnstoppedInstances(applicationShutdownInstances));
    }

    @Test
    void testIsTimeoutExceededWithTimeOutExceeded() {
        Instant timeBeforeTenMinutes = Instant.now()
                                              .minusSeconds(ShutdownUtil.TIMEOUT_IN_SECONDS);
        ApplicationShutdown applicationShutdownInstance = createApplicationShutdownInstance(true, Date.from(timeBeforeTenMinutes));

        assertTrue(ShutdownUtil.isTimeoutExceeded(applicationShutdownInstance));
    }

    @Test
    void testIsTimeoutExceededWithTimeOutNotExceeded() {
        Instant timeBeforeTenMinutes = Instant.now()
                                              .minusSeconds(TIMEOUT_IN_SECONDS);
        ApplicationShutdown applicationShutdownInstance = createApplicationShutdownInstance(true, Date.from(timeBeforeTenMinutes));

        assertFalse(ShutdownUtil.isTimeoutExceeded(applicationShutdownInstance));
    }

    @Test
    void testIsApplicationShutdownScheduledForMoreThanADay() {
        Instant timeBeforeTenMinutes = Instant.now()
                                              .minusSeconds(ShutdownUtil.DAY_IN_SECONDS);
        ApplicationShutdown applicationShutdownInstance = createApplicationShutdownInstance(true, Date.from(timeBeforeTenMinutes));

        assertTrue(ShutdownUtil.isApplicationShutdownScheduledForMoreThanADay(applicationShutdownInstance));
    }

    @Test
    void testIsApplicationShutdownScheduledForMoreThanADayWithTimeOutNotExceeded() {
        Instant timeBeforeTenMinutes = Instant.now()
                                              .minusSeconds(TIMEOUT_IN_SECONDS);
        ApplicationShutdown applicationShutdownInstance = createApplicationShutdownInstance(true, Date.from(timeBeforeTenMinutes));

        assertFalse(ShutdownUtil.isApplicationShutdownScheduledForMoreThanADay(applicationShutdownInstance));
    }

    private ApplicationShutdown createApplicationShutdownInstance(boolean isInstanceStopped, Date startedAt) {
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

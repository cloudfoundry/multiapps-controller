package org.cloudfoundry.multiapps.controller.shutdown.client;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.dto.ImmutableApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.query.impl.ApplicationShutdownQueryImpl;
import org.cloudfoundry.multiapps.controller.persistence.services.ApplicationShutdownService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationShutdownSchedulerTest {

    private final String APPLICATION_ID = UUID.randomUUID()
                                              .toString();
    private final String INSTANCE_ID = UUID.randomUUID()
                                           .toString();
    private final String INSTANCE_ID_2 = UUID.randomUUID()
                                             .toString();

    @Mock
    private ApplicationShutdownService applicationShutdownService;

    @Mock
    private ApplicationShutdownQueryImpl applicationShutdownQuery;

    private ApplicationShutdownScheduler applicationShutdownScheduler;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        applicationShutdownScheduler = new ApplicationShutdownScheduler(applicationShutdownService);
    }

    @Test
    void testScheduleApplicationForShutdownWithZeroInstances() {
        List<ApplicationShutdown> instances = applicationShutdownScheduler.scheduleApplicationForShutdown(APPLICATION_ID, 0);
        assertEquals(0, instances.size());
    }

    @Test
    void testScheduleApplicationForShutdownWithFiveInstances() {
        List<ApplicationShutdown> instances = applicationShutdownScheduler.scheduleApplicationForShutdown(APPLICATION_ID, 5);
        for (ApplicationShutdown instance : instances) {
            assertNotNull(instance.getId());
            assertNotNull(instance.getStartedAt());
            assertEquals(APPLICATION_ID, instance.getApplicationId());
        }
        assertEquals(5, instances.size());
    }

    @Test
    void testGetScheduledApplicationInstancesForShutdownWithId() {
        ApplicationShutdown applicationShutdown = createApplicationShutdownInstance(INSTANCE_ID);
        when(applicationShutdownQuery.id(anyString())).thenReturn(applicationShutdownQuery);
        when(applicationShutdownQuery.applicationId(anyString())).thenReturn(applicationShutdownQuery);
        when(applicationShutdownQuery.singleResult()).thenReturn(applicationShutdown);
        when(applicationShutdownService.createQuery()).thenReturn(applicationShutdownQuery);

        List<ApplicationShutdown> instances = applicationShutdownScheduler.getScheduledApplicationInstancesForShutdown(APPLICATION_ID,
                                                                                                                       List.of(
                                                                                                                           INSTANCE_ID));
        for (ApplicationShutdown instance : instances) {
            assertNotNull(instance.getId());
            assertNotNull(instance.getStartedAt());
            assertEquals(APPLICATION_ID, instance.getApplicationId());
        }

        assertEquals(1, instances.size());
        verify(applicationShutdownQuery).id(INSTANCE_ID);
        verify(applicationShutdownQuery).applicationId(APPLICATION_ID);
    }

    @Test
    void testGetScheduledApplicationInstancesForShutdownWithoutIds() {
        List<ApplicationShutdown> instances = applicationShutdownScheduler.getScheduledApplicationInstancesForShutdown(APPLICATION_ID,
                                                                                                                       List.of());

        assertEquals(0, instances.size());
    }

    private ApplicationShutdown createApplicationShutdownInstance(String instanceId) {
        return ImmutableApplicationShutdown.builder()
                                           .id(instanceId)
                                           .applicationId(APPLICATION_ID)
                                           .applicationInstanceIndex(0)
                                           .startedAt(Date.from(Instant.now()))
                                           .status(ApplicationShutdown.Status.FINISHED)
                                           .build();
    }
}

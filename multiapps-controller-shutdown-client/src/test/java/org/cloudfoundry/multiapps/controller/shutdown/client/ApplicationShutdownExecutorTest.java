package org.cloudfoundry.multiapps.controller.shutdown.client;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.dto.ImmutableApplicationShutdown;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationShutdownExecutorTest {

    private final ApplicationShutdownExecutor applicationShutdownExecutor = new ApplicationShutdownExecutorClone();

    @Mock
    private ApplicationShutdownScheduler applicationShutdownScheduler;

    private final String APPLICATION_ID = UUID.randomUUID()
                                              .toString();
    private final String INSTANCE_ID = UUID.randomUUID()
                                           .toString();
    private final String INSTANCE_ID_2 = UUID.randomUUID()
                                             .toString();
    private final String INSTANCE_ID_3 = UUID.randomUUID()
                                             .toString();
    private final int INSTANCE_COUNT = 5;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testExecuteWithStoppedInstances() {
        List<ApplicationShutdown> instances = List.of(createApplicationShutdownInstance(INSTANCE_ID));
        List<String> instanceIds = applicationShutdownExecutor.getApplicationShutdownInstancesIds(instances);

        when(applicationShutdownScheduler.scheduleApplicationForShutdown(APPLICATION_ID, INSTANCE_COUNT)).thenReturn(instances);
        when(applicationShutdownScheduler.getScheduledApplicationInstancesForShutdown(APPLICATION_ID, instanceIds)).thenReturn(instances);
        applicationShutdownExecutor.execute(APPLICATION_ID, INSTANCE_COUNT);

        verify(applicationShutdownScheduler).scheduleApplicationForShutdown(APPLICATION_ID, INSTANCE_COUNT);
        verify(applicationShutdownScheduler, times(1)).getScheduledApplicationInstancesForShutdown(APPLICATION_ID, instanceIds);
    }

    @Test
    void testGetApplicationShutdownInstancesIds() {
        List<ApplicationShutdown> instances = List.of(createApplicationShutdownInstance(INSTANCE_ID),
                                                      createApplicationShutdownInstance(INSTANCE_ID_2),
                                                      createApplicationShutdownInstance(INSTANCE_ID_3));

        List<String> ids = applicationShutdownExecutor.getApplicationShutdownInstancesIds(instances);
        assertEquals(3, ids.size());
        assertTrue(ids.contains(INSTANCE_ID));
        assertTrue(ids.contains(INSTANCE_ID_2));
        assertTrue(ids.contains(INSTANCE_ID_3));
    }

    class ApplicationShutdownExecutorClone extends ApplicationShutdownExecutor {

        @Override
        public ApplicationShutdownScheduler getApplicationShutdownScheduler() {
            return applicationShutdownScheduler;
        }
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

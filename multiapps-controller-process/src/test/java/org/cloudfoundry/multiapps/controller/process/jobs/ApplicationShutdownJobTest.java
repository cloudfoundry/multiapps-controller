package org.cloudfoundry.multiapps.controller.process.jobs;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.dto.ImmutableApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.query.ApplicationShutdownQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.ApplicationShutdownService;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationShutdownJobTest {

    @Mock
    private FlowableFacade flowableFacade;

    @Mock
    private ApplicationShutdownService applicationShutdownService;

    @Mock
    private ApplicationConfiguration applicationConfiguration;

    @Mock
    private ApplicationShutdownQuery applicationShutdownQuery;

    private ApplicationShutdownJob applicationShutdownJob;
    private final String APPLICATION_ID = UUID.randomUUID()
                                              .toString();
    private final String INSTANCE_ID = UUID.randomUUID()
                                           .toString();
    private final int APPLICATION_INSTANCE_INDEX = 0;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        applicationShutdownJob = new ApplicationShutdownJob(flowableFacade, applicationShutdownService, applicationConfiguration);

        when(applicationConfiguration.getApplicationGuid()).thenReturn(APPLICATION_ID);
        when(applicationConfiguration.getApplicationInstanceIndex()).thenReturn(APPLICATION_INSTANCE_INDEX);

        when(applicationShutdownService.createQuery()).thenReturn(applicationShutdownQuery);
        when(applicationShutdownQuery.applicationId(anyString())).thenReturn(applicationShutdownQuery);
        when(applicationShutdownQuery.applicationInstanceIndex(anyInt())).thenReturn(applicationShutdownQuery);

    }

    @Test
    void testRunWithNotStoppedFlowableExecutor() {
        ApplicationShutdown applicationShutdown = createApplicationShutdownInstance(ApplicationShutdown.Status.INITIAL);
        when(flowableFacade.isJobExecutorActive()).thenReturn(true);
        when(applicationShutdownQuery.singleResult()).thenReturn(applicationShutdown);

        applicationShutdownJob.run();

        verify(applicationShutdownService).update(any(), any());
    }

    @Test
    void testRunWithStoppedFlowableExecutor() {
        ApplicationShutdown applicationShutdown = createApplicationShutdownInstance(ApplicationShutdown.Status.INITIAL);
        when(applicationShutdownQuery.singleResult()).thenReturn(applicationShutdown);

        applicationShutdownJob.run();

        verify(applicationShutdownService, times(2)).update(any(), any());
    }

    @Test
    void testRunWithoutScheduledApplication() {
        applicationShutdownJob.run();

        verify(applicationShutdownService, times(0)).update(any(), any());
    }

    @Test
    void testRunWithScheduledApplicationInFinishedState() {
        ApplicationShutdown applicationShutdown = createApplicationShutdownInstance(ApplicationShutdown.Status.FINISHED);
        when(applicationShutdownQuery.singleResult()).thenReturn(applicationShutdown);
        applicationShutdownJob.run();

        verify(applicationShutdownService, times(0)).update(any(), any());
    }

    private ApplicationShutdown createApplicationShutdownInstance(ApplicationShutdown.Status status) {
        return ImmutableApplicationShutdown.builder()
                                           .id(INSTANCE_ID)
                                           .applicationId(APPLICATION_ID)
                                           .applicationInstanceIndex(APPLICATION_INSTANCE_INDEX)
                                           .startedAt(Date.from(Instant.now()))
                                           .status(status)
                                           .build();
    }
}

package org.cloudfoundry.multiapps.controller.web.bootstrap;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.dto.ApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.dto.ImmutableApplicationShutdown;
import org.cloudfoundry.multiapps.controller.persistence.query.ApplicationShutdownQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.ApplicationShutdownService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BootstrapServletTest {

    @Mock
    private ApplicationConfiguration applicationConfiguration;

    @Mock
    private ApplicationShutdownService applicationShutdownService;

    @Mock
    private ApplicationShutdownQuery applicationShutdownQuery;

    @InjectMocks
    private BootstrapServlet bootstrapServlet;
    private final int APPLICATION_INSTANCE_INDEX = 0;
    private final String APPLICATION_ID = UUID.randomUUID()
                                              .toString();
    private final String INSTANCE_ID = UUID.randomUUID()
                                           .toString();

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();

        when(applicationConfiguration.getApplicationInstanceIndex()).thenReturn(APPLICATION_INSTANCE_INDEX);
        when(applicationShutdownService.createQuery()).thenReturn(applicationShutdownQuery);
        when(applicationShutdownQuery.applicationId(anyString())).thenReturn(applicationShutdownQuery);
        when(applicationShutdownQuery.applicationInstanceIndex(anyInt())).thenReturn(applicationShutdownQuery);
    }

    @Test
    void testDeleteScheduledApplicationWithExistingApplication() {
        ApplicationShutdown applicationShutdown = createApplicationShutdownInstance();
        when(applicationShutdownQuery.singleResult()).thenReturn(applicationShutdown);

        bootstrapServlet.deleteOldScheduledApplication();
        verify(applicationShutdownService, times(2)).createQuery();
        verify(applicationShutdownQuery, times(2)).applicationInstanceIndex(anyInt());
        verify(applicationShutdownQuery).delete();
        verify(applicationShutdownQuery).singleResult();
    }

    @Test
    void testDeleteScheduledApplicationWithoutApplication() {
        bootstrapServlet.deleteOldScheduledApplication();
        verify(applicationShutdownService, times(1)).createQuery();
        verify(applicationShutdownQuery, times(1)).applicationInstanceIndex(anyInt());
        verify(applicationShutdownQuery, times(0)).delete();
        verify(applicationShutdownQuery, times(1)).singleResult();
    }

    private ApplicationShutdown createApplicationShutdownInstance() {
        return ImmutableApplicationShutdown.builder()
                                           .id(INSTANCE_ID)
                                           .applicationId(APPLICATION_ID)
                                           .applicationInstanceIndex(APPLICATION_INSTANCE_INDEX)
                                           .startedAt(Date.from(Instant.now()))
                                           .status(ApplicationShutdown.Status.INITIAL)
                                           .build();
    }
}

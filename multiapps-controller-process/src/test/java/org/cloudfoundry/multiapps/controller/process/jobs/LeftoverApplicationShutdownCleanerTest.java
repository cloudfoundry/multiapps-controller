package org.cloudfoundry.multiapps.controller.process.jobs;

import java.time.Instant;
import java.time.LocalDateTime;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LeftoverApplicationShutdownCleanerTest {

    @Mock
    private ApplicationShutdownService applicationShutdownService;

    @Mock
    private ApplicationShutdownQueryImpl applicationShutdownQuery;

    private LeftoverApplicationShutdownCleaner leftoverApplicationShutdownCleaner;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        leftoverApplicationShutdownCleaner = new LeftoverApplicationShutdownCleaner(applicationShutdownService);
        when(applicationShutdownService.createQuery()).thenReturn(applicationShutdownQuery);
    }

    @Test
    void testExecuteWithoutScheduledApplications() {
        when(applicationShutdownQuery.list()).thenReturn(List.of());
        leftoverApplicationShutdownCleaner.execute(LocalDateTime.now());
        verify(applicationShutdownService).createQuery();
        verify(applicationShutdownQuery).list();
    }

    @Test
    void testExecuteWithScheduledApplicationButNoLeftovers() {
        ApplicationShutdown applicationShutdown = createApplicationShutdown(Date.from(Instant.now()));
        when(applicationShutdownQuery.list()).thenReturn(List.of(applicationShutdown));
        leftoverApplicationShutdownCleaner.execute(LocalDateTime.now());
        verify(applicationShutdownService).createQuery();
        verify(applicationShutdownQuery).list();
    }

    @Test
    void testExecuteWithLeftoverApplications() {
        Date timeBeforeTwoDays = new Date(1738061834);
        ApplicationShutdown applicationShutdown = createApplicationShutdown(timeBeforeTwoDays);
        when(applicationShutdownQuery.list()).thenReturn(List.of(applicationShutdown));
        when(applicationShutdownQuery.id(any())).thenReturn(applicationShutdownQuery);

        leftoverApplicationShutdownCleaner.execute(LocalDateTime.now());

        verify(applicationShutdownService, times(2)).createQuery();
        verify(applicationShutdownQuery).list();
        verify(applicationShutdownQuery).delete();
    }

    private ApplicationShutdown createApplicationShutdown(Date startedAt) {
        return ImmutableApplicationShutdown.builder()
                                           .applicationInstanceIndex(0)
                                           .applicationId(UUID.randomUUID()
                                                              .toString())
                                           .id(UUID.randomUUID()
                                                   .toString())
                                           .startedAt(startedAt)
                                           .build();
    }
}

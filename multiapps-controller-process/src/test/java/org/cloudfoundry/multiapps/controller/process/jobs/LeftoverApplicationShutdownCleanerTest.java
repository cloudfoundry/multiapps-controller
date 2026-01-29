package org.cloudfoundry.multiapps.controller.process.jobs;

import java.time.LocalDateTime;

import org.cloudfoundry.multiapps.controller.persistence.query.impl.ApplicationShutdownQueryImpl;
import org.cloudfoundry.multiapps.controller.persistence.services.ApplicationShutdownService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
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
    void testExecuteWithLeftoverApplications() {
        when(applicationShutdownQuery.startedAtBefore(any())).thenReturn(applicationShutdownQuery);

        leftoverApplicationShutdownCleaner.execute(LocalDateTime.now());

        verify(applicationShutdownQuery).delete();
    }
}

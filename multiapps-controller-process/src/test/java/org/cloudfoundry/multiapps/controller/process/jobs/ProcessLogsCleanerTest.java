package org.cloudfoundry.multiapps.controller.process.jobs;

import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogsPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ProcessLogsCleanerTest {

    private static final LocalDateTime EXPIRATION_TIME = LocalDateTime.ofInstant(Instant.ofEpochMilli(5000), ZoneId.systemDefault());

    @Mock
    private ProcessLogsPersistenceService processLogsPersistenceService;
    @InjectMocks
    private ProcessLogsCleaner cleaner;

    @BeforeEach
    void initMocks() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testExecute() throws FileStorageException {
        cleaner.execute(EXPIRATION_TIME);
        verify(processLogsPersistenceService).deleteModifiedBefore(EXPIRATION_TIME);
    }

}

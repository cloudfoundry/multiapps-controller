package com.sap.cloud.lm.sl.cf.process.jobs;

import static org.mockito.Mockito.verify;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLogsPersistenceService;

public class ProcessLogsCleanerTest {

    private static final Date EXPIRATION_TIME = new Date(5000);

    @Mock
    private ProcessLogsPersistenceService processLogsPersistenceService;
    @InjectMocks
    private ProcessLogsCleaner cleaner;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testExecute() throws FileStorageException {
        cleaner.execute(EXPIRATION_TIME);
        verify(processLogsPersistenceService).deleteByModificationTime(EXPIRATION_TIME);
    }

}

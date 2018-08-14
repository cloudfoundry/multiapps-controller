package com.sap.cloud.lm.sl.cf.process.jobs;

import static org.mockito.Mockito.verify;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.JobExecutionException;

import com.sap.cloud.lm.sl.cf.persistence.services.AbstractFileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;

public class FilesCleanerTest {

    private static final Date EXPIRATION_TIME = new Date(5000);

    @Mock
    private AbstractFileService fileService;
    @InjectMocks
    private FilesCleaner cleaner;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testRemoveOldFiles() throws JobExecutionException, FileStorageException {
        cleaner.execute(EXPIRATION_TIME);
        verify(fileService).deleteByModificationTime(EXPIRATION_TIME);
    }

}

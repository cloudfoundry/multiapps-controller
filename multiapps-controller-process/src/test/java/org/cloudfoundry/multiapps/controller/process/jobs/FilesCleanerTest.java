package org.cloudfoundry.multiapps.controller.process.jobs;

import static org.mockito.Mockito.verify;

import java.util.Date;

import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.jobs.FilesCleaner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class FilesCleanerTest {

    private static final Date EXPIRATION_TIME = new Date(5000);

    @Mock
    private FileService fileService;
    @InjectMocks
    private FilesCleaner cleaner;

    @BeforeEach
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testExecute() throws FileStorageException {
        cleaner.execute(EXPIRATION_TIME);
        verify(fileService).deleteModifiedBefore(EXPIRATION_TIME);
    }

}

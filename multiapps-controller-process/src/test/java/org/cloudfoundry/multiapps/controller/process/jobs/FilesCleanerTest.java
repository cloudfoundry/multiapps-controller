package org.cloudfoundry.multiapps.controller.process.jobs;

import static org.mockito.Mockito.verify;

import java.util.Date;

import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class FilesCleanerTest {

    private static final Date EXPIRATION_TIME = new Date(5000);

    @Mock
    private FileService fileService;
    @InjectMocks
    private FilesCleaner cleaner;

    @BeforeEach
    void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testExecute() throws FileStorageException {
        cleaner.execute(EXPIRATION_TIME);
        verify(fileService).deleteModifiedBefore(EXPIRATION_TIME);
    }

}

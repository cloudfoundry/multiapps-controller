package org.cloudfoundry.multiapps.controller.process.jobs;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.cloudfoundry.multiapps.controller.persistence.query.AsyncUploadJobsQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.AsyncUploadJobService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class FilesCleanerTest {

    private static final LocalDateTime EXPIRATION_TIME = LocalDateTime.ofInstant(Instant.ofEpochMilli(5000), ZoneId.systemDefault());

    @Mock
    private FileService fileService;
    @Mock
    private AsyncUploadJobService uploadJobService;
    @Mock(answer = Answers.RETURNS_SELF)
    private AsyncUploadJobsQuery query;
    @InjectMocks
    private FilesCleaner cleaner;

    @BeforeEach
    void initMocks() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testExecute() throws FileStorageException {
        when(uploadJobService.createQuery()).thenReturn(query);
        cleaner.execute(EXPIRATION_TIME);
        verify(fileService).deleteModifiedBefore(EXPIRATION_TIME);
        verify(query, Mockito.atLeastOnce()).delete();
    }

}

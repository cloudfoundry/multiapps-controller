package org.cloudfoundry.multiapps.controller.process.jobs;

import static org.mockito.ArgumentMatchers.any;

import java.util.List;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.cloudfoundry.multiapps.controller.persistence.query.AsyncUploadJobsQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.AsyncUploadJobService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class OrphanedFilesCleanerTest {

    @Mock
    private FileService fileService;
    @Mock
    private AsyncUploadJobService asyncUploadJobService;
    @Mock
    private ApplicationConfiguration applicationConfiguration;
    @Mock(answer = Answers.RETURNS_SELF)
    private AsyncUploadJobsQuery asyncUploadJobsQuery;

    private OrphanedFilesCleaner orphanedFilesCleaner;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        orphanedFilesCleaner = new OrphanedFilesCleaner(fileService, asyncUploadJobService, applicationConfiguration);
    }

    @Test
    void testRunningOnAnotherInstance() throws FileStorageException {
        prepareApplicationConfiguration(0);
        orphanedFilesCleaner.clean();
        Mockito.verify(fileService, Mockito.never())
               .deleteFilesByIds(any());
        Mockito.verify(asyncUploadJobService, Mockito.never())
               .createQuery();
    }

    private void prepareApplicationConfiguration(int instanceIndex) {
        Mockito.when(applicationConfiguration.getApplicationInstanceIndex())
               .thenReturn(instanceIndex);
    }

    @Test
    void testClean() throws FileStorageException {
        prepareApplicationConfiguration(1);
        prepareFileService();
        prepareAsyncUploadJobService();
        orphanedFilesCleaner.clean();
        Mockito.verify(fileService)
               .deleteFilesByIds(List.of("1", "2"));
        Mockito.verify(asyncUploadJobsQuery)
               .delete();
    }

    private void prepareFileService() throws FileStorageException {
        FileEntry file1 = ImmutableFileEntry.builder()
                                            .id("1")
                                            .name("file-1")
                                            .operationId("op-1")
                                            .build();
        FileEntry file2 = ImmutableFileEntry.builder()
                                            .id("2")
                                            .name("file-2")
                                            .operationId("op-2")
                                            .build();
        Mockito.when(fileService.listFilesCreatedAfterAndBeforeWithoutOperationId(any(), any()))
               .thenReturn(List.of(file1, file2));
    }

    private void prepareAsyncUploadJobService() {
        Mockito.when(asyncUploadJobService.createQuery())
               .thenReturn(asyncUploadJobsQuery);
    }
}

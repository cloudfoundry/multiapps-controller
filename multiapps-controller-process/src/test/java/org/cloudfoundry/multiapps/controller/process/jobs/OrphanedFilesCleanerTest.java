package org.cloudfoundry.multiapps.controller.process.jobs;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.cloudfoundry.multiapps.controller.persistence.query.OperationQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class OrphanedFilesCleanerTest {

    @Mock
    private FileService fileService;
    @Mock
    private ApplicationConfiguration configuration;
    @Mock
    private FlowableFacade flowableFacade;
    @Mock
    private OperationService operationService;
    @Mock(answer = Answers.RETURNS_SELF)
    private OperationQuery query;
    @InjectMocks
    private OrphanedFilesCleaner cleaner;

    @BeforeEach
    void initMocks() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @Test
    void testWithOrphanedFiles() throws FileStorageException {
        when(configuration.getApplicationInstanceIndex()).thenReturn(1);
        when(query.list()).thenReturn(List.of());
        when(operationService.createQuery()).thenReturn(query);
        when(fileService.listFilesCreatedAfterAndBefore(any(), any())).thenReturn(List.of(createFileEntry("id-1", "space-1"),
                                                                          createFileEntry("id-2", "space-2")));

        cleaner.run();

        verify(fileService, times(2)).deleteFile(any(), any());
    }

    @Test
    void testWithoutOrphanedFiles() throws FileStorageException {
        when(configuration.getApplicationInstanceIndex()).thenReturn(1);
        when(query.list()).thenReturn(List.of(createOperation("process-1"), createOperation("process-2"), createOperation("process-3")));
        when(operationService.createQuery())
                             .thenReturn(query);
        when(fileService.listFilesCreatedAfterAndBefore(any(), any())).thenReturn(List.of(createFileEntry("id-1", "space-1"),
                createFileEntry("id-2", "space-2"),
                createFileEntry("split-1","space-3"),
                createFileEntry("split-2", "space-3")));
        var histVar1 = mock(HistoricVariableInstance.class);
        when(histVar1.getValue()).thenReturn("id-1");
        var histVar2 = mock(HistoricVariableInstance.class);
        when(histVar2.getValue()).thenReturn("id-2");
        var histVar3 = mock(HistoricVariableInstance.class);
        when(histVar3.getValue()).thenReturn("split-1,split-2");
        when(flowableFacade.getHistoricVariableInstance(eq("process-1"), any())).thenReturn(histVar1);
        when(flowableFacade.getHistoricVariableInstance(eq("process-2"), any())).thenReturn(histVar2);
        when(flowableFacade.getHistoricVariableInstance(eq("process-3"), any())).thenReturn(histVar3);

        cleaner.run();

        verify(fileService, never()).deleteFile(any(), any());
    }

    private Operation createOperation(String processId) {
        return ImmutableOperation.builder()
                                 .processId(processId)
                                 .build();
    }

    private FileEntry createFileEntry(String id, String space) {
        return ImmutableFileEntry.builder()
                                 .id(id)
                                 .space(space)
                                 .build();
    }
}

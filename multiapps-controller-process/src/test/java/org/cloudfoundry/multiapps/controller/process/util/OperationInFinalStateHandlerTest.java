package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.api.model.Operation.State;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.dynatrace.DynatraceProcessDuration;
import org.cloudfoundry.multiapps.controller.process.dynatrace.DynatracePublisher;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class OperationInFinalStateHandlerTest {

    private static final ProcessType PROCESS_TYPE = ProcessType.DEPLOY;
    private static final State OPERATION_STATE = State.FINISHED;
    private static final String SPACE_ID = "space-id";
    private static final String MTA_ID = "my-mta";
    private static final String PROCESS_ID = "xxx-yyy-zzz";
    private static final long PROCESS_DURATION = 1000;

    private final DelegateExecution execution = MockDelegateExecution.createSpyInstance();

    @Mock
    private FileService fileService;
    @Mock
    private StepLogger.Factory stepLoggerFactory;
    @Mock
    private StepLogger stepLogger;
    @Mock
    private DynatracePublisher dynatracePublisher;
    @Mock
    private OperationTimeAggregator operationTimeAggregator;
    @Mock
    private ProcessTime processTime;

    @InjectMocks
    private final OperationInFinalStateHandler eventHandler = new OperationInFinalStateHandler();

    public static Stream<Arguments> testHandle() {
        return Stream.of(
//@formatter:off
          Arguments.of("10", "20", true, new String[] { }),
          Arguments.of("10", null, true, new String[] { }),
          Arguments.of(null, "20", true, new String[] { }),
          Arguments.of(null, null, true, new String[] { }),
          Arguments.of("10", "20", false, new String[] { "10", "20", }),
          Arguments.of(null, "20", false, new String[] { "20", }),
          Arguments.of("10", null, false, new String[] { "10", }),
          Arguments.of(null, null, false, new String[] { }),
          Arguments.of("10,20,30", null, false, new String[] { "10", "20", "30", }),
          Arguments.of(null, "10,20,30", false, new String[] { "10", "20", "30", })
//@formatter:on
        );
    }

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        Mockito.when(stepLoggerFactory.create(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
               .thenReturn(stepLogger);
    }

    @ParameterizedTest
    @MethodSource
    void testHandle(String archiveIds, String extensionDescriptorIds, boolean keepFiles, String[] expectedFileIdsToSweep) throws Exception {
        prepareContext(archiveIds, extensionDescriptorIds, keepFiles);
        prepareOperationTimeAggregator();

        eventHandler.handle(execution, PROCESS_TYPE, OPERATION_STATE);

        verifyDeleteDeploymentFiles(expectedFileIdsToSweep);
        verifyDynatracePublisher();
    }

    private void prepareContext(String archiveIds, String extensionDescriptorIds, boolean keepFiles) {
        VariableHandling.set(execution, Variables.SPACE_GUID, SPACE_ID);
        VariableHandling.set(execution, Variables.CORRELATION_ID, PROCESS_ID);
        VariableHandling.set(execution, Variables.MTA_ID, MTA_ID);
        VariableHandling.set(execution, Variables.APP_ARCHIVE_ID, archiveIds);
        VariableHandling.set(execution, Variables.EXT_DESCRIPTOR_FILE_ID, extensionDescriptorIds);
        VariableHandling.set(execution, Variables.KEEP_FILES, keepFiles);
    }

    private void prepareOperationTimeAggregator() {
        Mockito.when(operationTimeAggregator.computeOverallProcessTime(Mockito.eq(PROCESS_ID), Mockito.any()))
               .thenReturn(processTime);
        Mockito.when(processTime.getProcessDuration())
               .thenReturn(PROCESS_DURATION);
    }

    private void verifyDeleteDeploymentFiles(String[] expectedFileIdsToSweep) throws FileStorageException {
        for (String fileId : expectedFileIdsToSweep) {
            Mockito.verify(fileService)
                   .deleteFile(SPACE_ID, fileId);
        }
    }

    private void verifyDynatracePublisher() {
        ArgumentCaptor<DynatraceProcessDuration> argumentCaptor = ArgumentCaptor.forClass(DynatraceProcessDuration.class);
        Mockito.verify(dynatracePublisher)
               .publishProcessDuration(argumentCaptor.capture(), Mockito.any());
        DynatraceProcessDuration actualDynatraceEvent = argumentCaptor.getValue();
        assertEquals(PROCESS_ID, actualDynatraceEvent.getProcessId());
        assertEquals(MTA_ID, actualDynatraceEvent.getMtaId());
        assertEquals(SPACE_ID, actualDynatraceEvent.getSpaceId());
        assertEquals(PROCESS_TYPE, actualDynatraceEvent.getProcessType());
        assertEquals(OPERATION_STATE, actualDynatraceEvent.getOperationState());
        assertEquals(PROCESS_DURATION, actualDynatraceEvent.getProcessDuration());
    }

}

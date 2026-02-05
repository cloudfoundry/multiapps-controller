package org.cloudfoundry.multiapps.controller.process.util;

import java.time.ZonedDateTime;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.Operation.State;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.persistence.model.FileEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableFileEntry;
import org.cloudfoundry.multiapps.controller.persistence.query.impl.OperationQueryImpl;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.process.dynatrace.DynatraceProcessDuration;
import org.cloudfoundry.multiapps.controller.process.dynatrace.DynatracePublisher;
import org.cloudfoundry.multiapps.controller.process.security.store.SecretTokenStoreDeletion;
import org.cloudfoundry.multiapps.controller.process.security.store.SecretTokenStoreFactory;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

class OperationInFinalStateHandlerTest {

    private static final ProcessType PROCESS_TYPE = ProcessType.DEPLOY;
    private static final State OPERATION_STATE = State.FINISHED;
    private static final String SPACE_ID = "space-id";
    private static final String MTA_ID = "my-mta";
    private static final String PROCESS_ID = "xxx-yyy-zzz";
    private static final String PROCESS_ID_2 = "zzz-xxx-yyy";
    private static final String USER_GUID = "test-user";
    private static final long PROCESS_DURATION = 1000;
    private static final String DISPOSABLE_USER_PROVIDED_SERVICE_NAME = "__mta-secure-my-mta-fake343";

    private static final Operation OPERATION = createOperation("1", ProcessType.DEPLOY, "spaceId", "mtaId", "user", true,
                                                               ZonedDateTime.parse("2010-10-08T10:00:00.000Z[UTC]"),
                                                               ZonedDateTime.parse("2010-10-14T10:00:00.000Z[UTC]"), State.RUNNING);

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
    @Mock
    private OperationService operationService;
    @Mock
    private SecretTokenStoreFactory secretTokenStoreFactory;
    @Mock
    private SecretTokenStoreDeletion secretTokenStoreDeletion;
    @Mock
    private CloudControllerClientProvider cloudControllerClientProvider;
    @Mock
    private CloudControllerClient cloudControllerClient;

    @InjectMocks
    private final OperationInFinalStateHandler eventHandler = new OperationInFinalStateHandler();

    public static Stream<Arguments> testHandle() {
        return Stream.of(
            //@formatter:off
          Arguments.of("10", "20", PROCESS_ID, true, new String[] { }),
          Arguments.of("10", null, PROCESS_ID, true, new String[] { }),
          Arguments.of(null, "20", PROCESS_ID, true, new String[] { }),
          Arguments.of(null, null, PROCESS_ID, true, new String[] { }),
          Arguments.of("10", "20", PROCESS_ID, false, new String[] { "10", "20", }),
          Arguments.of(null, "20", PROCESS_ID, false, new String[] { "20", }),
          Arguments.of("10", null, PROCESS_ID, false, new String[] { "10", }),
          Arguments.of(null, null, PROCESS_ID, false, new String[] { }),
          Arguments.of("10,20,30", null, PROCESS_ID, false, new String[] { "10", "20", "30", }),
          Arguments.of(null, "10,20,30", PROCESS_ID, false, new String[] { "10", "20", "30", }),
          Arguments.of("10,20,30", "40,50", PROCESS_ID_2, false, new String[] {})
//@formatter:on
        );
    }

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        Mockito.when(stepLoggerFactory.create(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
               .thenReturn(stepLogger);
        Mockito.when(secretTokenStoreFactory.createSecretTokenStoreDeletionRelated())
               .thenReturn(secretTokenStoreDeletion);
    }

    @ParameterizedTest
    @MethodSource
    void testHandle(String archiveIds, String extensionDescriptorIds, String fileOwnershipProcessId, boolean keepFiles,
                    String[] expectedFileIdsToSweep)
        throws Exception {
        prepareContext(archiveIds, extensionDescriptorIds, keepFiles);
        prepareOperationTimeAggregator();
        prepareOperationService();
        prepareFileService(archiveIds, extensionDescriptorIds, fileOwnershipProcessId);

        eventHandler.handle(execution, PROCESS_TYPE, OPERATION_STATE);

        verifyOperationSetState();
        verifyDeleteDeploymentFiles(expectedFileIdsToSweep);
        verifyDynatracePublisher();
        verify(secretTokenStoreDeletion).deleteByProcessInstanceId(PROCESS_ID);
    }

    @Test
    void testDeleteSecretTokensForProcessWhenOperationStateNotFinished() {
        prepareContext(null, null, true);
        prepareOperationTimeAggregator();
        prepareOperationService();

        eventHandler.handle(execution, PROCESS_TYPE, State.ACTION_REQUIRED);

        verify(secretTokenStoreFactory, atLeastOnce()).createSecretTokenStoreDeletionRelated();
    }

    @Test
    void testDeleteDisposableUserProvidedServiceWhenEnabled() {
        prepareContext(null, null, true);
        prepareOperationTimeAggregator();
        prepareOperationService();

        VariableHandling.set(execution, Variables.IS_DISPOSABLE_USER_PROVIDED_SERVICE_ENABLED, Boolean.TRUE);
        VariableHandling.set(execution, Variables.DISPOSABLE_USER_PROVIDED_SERVICE_NAME, DISPOSABLE_USER_PROVIDED_SERVICE_NAME);
        VariableHandling.set(execution, Variables.USER_GUID, USER_GUID);

        Mockito.when(cloudControllerClientProvider.getControllerClient(anyString(), anyString(), anyString()))
               .thenReturn(cloudControllerClient);
        eventHandler.handle(execution, PROCESS_TYPE, OPERATION_STATE);

        verify(cloudControllerClient).deleteServiceInstance(DISPOSABLE_USER_PROVIDED_SERVICE_NAME);
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

    private void prepareOperationService() {
        OperationQueryImpl operationQuery = Mockito.mock(OperationQueryImpl.class);
        Mockito.when(operationService.createQuery())
               .thenReturn(operationQuery);
        Mockito.when(operationQuery.processId(Mockito.any()))
               .thenReturn(operationQuery);
        Mockito.when(operationQuery.singleResult())
               .thenReturn(OPERATION);
    }

    private void prepareFileService(String archiveIds, String extensionDescriptorIds, String fileOwnershipProcessId)
        throws FileStorageException {
        prepareFileService(archiveIds, fileOwnershipProcessId);
        prepareFileService(extensionDescriptorIds, fileOwnershipProcessId);
    }

    private void prepareFileService(String fileIds, String fileOwnershipProcessId) throws FileStorageException {
        if (fileIds == null) {
            return;
        }
        for (String fileId : fileIds.split(",")) {
            FileEntry entry = ImmutableFileEntry.builder()
                                                .id(fileId)
                                                .operationId(fileOwnershipProcessId)
                                                .build();
            Mockito.when(fileService.getFile(SPACE_ID, fileId))
                   .thenReturn(entry);
        }
    }

    private void verifyDeleteDeploymentFiles(String[] expectedFileIdsToSweep) throws FileStorageException {
        for (String fileId : expectedFileIdsToSweep) {
            verify(fileService)
                .deleteFile(SPACE_ID, fileId);
        }
    }

    private void verifyOperationSetState() {
        ArgumentCaptor<ImmutableOperation> arg = ArgumentCaptor.forClass(ImmutableOperation.class);
        verify(operationService)
            .update(Mockito.any(), arg.capture());
        Operation updatedOperation = arg.getValue();
        assertEquals(OPERATION_STATE, updatedOperation.getState());
        assertFalse(updatedOperation.hasAcquiredLock());
    }

    private void verifyDynatracePublisher() {
        ArgumentCaptor<DynatraceProcessDuration> argumentCaptor = ArgumentCaptor.forClass(DynatraceProcessDuration.class);
        verify(dynatracePublisher)
            .publishProcessDuration(argumentCaptor.capture(), Mockito.any());
        DynatraceProcessDuration actualDynatraceEvent = argumentCaptor.getValue();
        assertEquals(PROCESS_ID, actualDynatraceEvent.getProcessId());
        assertEquals(MTA_ID, actualDynatraceEvent.getMtaId());
        assertEquals(SPACE_ID, actualDynatraceEvent.getSpaceId());
        assertEquals(PROCESS_TYPE, actualDynatraceEvent.getProcessType());
        assertEquals(OPERATION_STATE, actualDynatraceEvent.getOperationState());
        assertEquals(PROCESS_DURATION, actualDynatraceEvent.getProcessDuration());
    }

    private static Operation createOperation(String processId, ProcessType type, String spaceId, String mtaId, String user,
                                             boolean acquiredLock, ZonedDateTime startedAt, ZonedDateTime endedAt, Operation.State state) {
        return ImmutableOperation.builder()
                                 .processId(processId)
                                 .processType(type)
                                 .spaceId(spaceId)
                                 .mtaId(mtaId)
                                 .user(user)
                                 .hasAcquiredLock(acquiredLock)
                                 .startedAt(startedAt)
                                 .endedAt(endedAt)
                                 .state(state)
                                 .build();
    }

}

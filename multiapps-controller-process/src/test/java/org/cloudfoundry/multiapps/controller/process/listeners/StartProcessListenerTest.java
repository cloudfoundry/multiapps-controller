package org.cloudfoundry.multiapps.controller.process.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.test.GenericArgumentMatcher;
import org.cloudfoundry.multiapps.controller.api.model.ImmutableOperation;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.api.model.ProcessType;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.query.OperationQuery;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogsPersistenceService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogsPersister;
import org.cloudfoundry.multiapps.controller.process.dynatrace.DynatraceProcessEvent;
import org.cloudfoundry.multiapps.controller.process.dynatrace.DynatracePublisher;
import org.cloudfoundry.multiapps.controller.process.metadata.ProcessTypeToOperationMetadataMapper;
import org.cloudfoundry.multiapps.controller.process.steps.StepsUtil;
import org.cloudfoundry.multiapps.controller.process.util.MockDelegateExecution;
import org.cloudfoundry.multiapps.controller.process.util.ProcessTypeParser;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

class StartProcessListenerTest {

    private static final String SPACE_ID = "9ba1dfc7-9c2c-40d5-8bf9-fd04fa7a1722";
    private static final String TASK_ID = "test-task-id";
    private static final String USER = "current-user";
    private final static String MTA_ID = "my-mta";
    private static final ZonedDateTime START_TIME = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.of("UTC"));

    private String processInstanceId;
    private ProcessType processType;

    private final DelegateExecution execution = MockDelegateExecution.createSpyInstance();

    @Mock
    private OperationService operationService;
    @Mock(answer = Answers.RETURNS_SELF)
    private OperationQuery operationQuery;
    @Mock
    private StepLogger.Factory stepLoggerFactory;
    @Mock
    private StepLogger stepLogger;
    @Mock
    private ProcessTypeParser processTypeParser;
    @Mock
    private ProcessLogsPersistenceService processLogsPersistenceService;
    @Spy
    private final ProcessLogsPersister processLogsPersister = new ProcessLogsPersister();
    @Mock
    private ApplicationConfiguration configuration;
    @Mock
    private DynatracePublisher dynatracePublisher;
    @Mock
    private HistoricOperationEventService historicOperationEventService;
    @Spy
    private ProcessTypeToOperationMetadataMapper operationMetadataMapper;

    private final Supplier<ZonedDateTime> currentTimeSupplier = () -> START_TIME;

    @InjectMocks
    private final StartProcessListener listener = new StartProcessListener();

    static Stream<Arguments> testVerify() {
        return Stream.of(
                         // (0) Create Operation for process undeploy
                         Arguments.of("process-instance-id", ProcessType.UNDEPLOY),
                         // (1) Create Operation for process deploy
                         Arguments.of("process-instance-id", ProcessType.DEPLOY));
    }

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    @ParameterizedTest
    @MethodSource
    void testVerify(String processInstanceId, ProcessType processType) throws Exception {
        this.processType = processType;
        this.processInstanceId = processInstanceId;
        prepare();
        listener.notify(execution);

        verifyOperationInsertion();
        verifyDynatracePublishEvent();
    }

    private void verifyDynatracePublishEvent() {
        ArgumentCaptor<DynatraceProcessEvent> argumentCaptor = ArgumentCaptor.forClass(DynatraceProcessEvent.class);
        Mockito.verify(dynatracePublisher).publishProcessEvent(argumentCaptor.capture(), Mockito.any());
        DynatraceProcessEvent actualDynatraceEvent = argumentCaptor.getValue();
        assertEquals(MTA_ID, actualDynatraceEvent.getMtaId());
        assertEquals(SPACE_ID, actualDynatraceEvent.getSpaceId());
        assertEquals(processType, actualDynatraceEvent.getProcessType());
        assertEquals(DynatraceProcessEvent.EventType.STARTED, actualDynatraceEvent.getEventType());
    }

    private void prepare() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        prepareContext();
        Mockito.when(stepLoggerFactory.create(any(), any(), any(), any()))
               .thenReturn(stepLogger);
        Mockito.doNothing()
               .when(processLogsPersister)
               .persistLogs(processInstanceId, TASK_ID);
        Mockito.when(operationService.createQuery())
               .thenReturn(operationQuery);
        Mockito.doReturn(null)
               .when(operationQuery)
               .singleResult();
    }

    private void prepareContext() {
        listener.currentTimeSupplier = currentTimeSupplier;
        Mockito.when(execution.getProcessInstanceId())
               .thenReturn(processInstanceId);
        Mockito.when(execution.getVariables())
               .thenReturn(Collections.emptyMap());
        Mockito.when(processTypeParser.getProcessType(execution))
               .thenReturn(processType);
        VariableHandling.set(execution, Variables.SPACE_GUID, SPACE_ID);
        VariableHandling.set(execution, Variables.MTA_ID, MTA_ID);
        VariableHandling.set(execution, Variables.USER, USER);
        VariableHandling.set(execution, Variables.CORRELATION_ID, processInstanceId);
        VariableHandling.set(execution, Variables.TASK_ID, TASK_ID);
    }

    private void verifyOperationInsertion() throws SLException {
        String user = StepsUtil.determineCurrentUser(execution);
        Operation operation = ImmutableOperation.builder()
                                                .processId(processInstanceId)
                                                .processType(processType)
                                                .spaceId(SPACE_ID)
                                                .startedAt(START_TIME)
                                                .user(user)
                                                .hasAcquiredLock(false)
                                                .state(Operation.State.RUNNING)
                                                .build();
        Mockito.verify(operationService)
               .add(Mockito.argThat(GenericArgumentMatcher.forObject(operation)));
        Mockito.verify(processLogsPersister, Mockito.atLeastOnce())
               .persistLogs(processInstanceId, TASK_ID);
    }

}

package org.cloudfoundry.multiapps.controller.process.steps;

import static org.mockito.ArgumentMatchers.any;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.model.ErrorType;
import org.cloudfoundry.multiapps.controller.persistence.model.HistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableHistoricOperationEvent;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogger;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogsPersister;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ProcessHelper;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ExecutionQuery;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class ProcessStepHelperTest {

    private static final String CORRELATION_GUID = UUID.randomUUID()
                                                       .toString();
    private static final String TASK_GUID = UUID.randomUUID()
                                                .toString();

    @Mock
    private ProgressMessageService progressMessageService;
    @Mock
    private ProcessLogsPersister processLogsPersister;
    @Mock
    private StepLogger stepLogger;
    @Mock
    private ProcessEngineConfiguration processEngineConfiguration;
    @Mock
    private ProcessHelper processHelper;
    @Mock
    private ProcessContext context;
    @Mock
    private DelegateExecution execution;
    private ProcessStepHelper processStepHelper;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        prepareStepLogger();
        prepareContext();
        processStepHelper = ImmutableProcessStepHelper.builder()
                                                      .progressMessageService(progressMessageService)
                                                      .stepLogger(stepLogger)
                                                      .processLogsPersister(processLogsPersister)
                                                      .processEngineConfiguration(processEngineConfiguration)
                                                      .processHelper(processHelper)
                                                      .build();
    }

    @Test
    void testPostExecutionStep() {
        Mockito.when(context.getVariable(Variables.TASK_ID))
               .thenReturn(TASK_GUID);
        FlowElement flowElement = Mockito.mock(SubProcess.class);
        Mockito.when(execution.getCurrentFlowElement())
               .thenReturn(flowElement);

        processStepHelper.postExecuteStep(context, StepPhase.DONE);
        Mockito.verify(processLogsPersister)
               .persistLogs(CORRELATION_GUID, TASK_GUID);
        Mockito.verify(context)
               .setVariable(Variables.STEP_EXECUTION, StepPhase.DONE.toString());
    }

    @Test
    void testPreExecuteStepWithoutError() {
        Mockito.when(execution.getCurrentActivityId())
               .thenReturn("activityId");
        processStepHelper.preExecuteStep(context, StepPhase.EXECUTE);
        Mockito.verify(context)
               .setVariable(Variables.TASK_ID, "activityId");
        Mockito.verify(context)
               .setVariable(Variables.STEP_PHASE, StepPhase.EXECUTE);
    }

    @Test
    void testPreExecuteStepWithError() {
        Mockito.when(execution.getCurrentActivityId())
               .thenReturn("activityId");
        Mockito.when(context.getVariable(Variables.ERROR_TYPE))
               .thenReturn(ErrorType.CONTENT_ERROR);
        processStepHelper.preExecuteStep(context, StepPhase.DONE);
        Mockito.verify(context)
               .setVariable(Variables.TASK_ID, "activityId");
        Mockito.verify(context)
               .setVariable(Variables.STEP_PHASE, StepPhase.DONE);
        Mockito.verify(context)
               .removeVariable(Variables.ERROR_TYPE);
    }

    @Test
    void testLogExceptionAndStoreExceptionContentError() {
        prepareProgressMessagesService();
        processStepHelper.logExceptionAndStoreProgressMessage(context, new ContentException("content exception"));
        Mockito.verify(context)
               .setVariable(Variables.ERROR_TYPE, ErrorType.CONTENT_ERROR);
        Mockito.verify(progressMessageService)
               .add(any());
    }

    @Test
    void testLogExceptionAndStoreExceptionUnknownError() {
        prepareProgressMessagesService();
        processStepHelper.logExceptionAndStoreProgressMessage(context, new RuntimeException("runtime exception"));
        Mockito.verify(context)
               .setVariable(Variables.ERROR_TYPE, ErrorType.UNKNOWN_ERROR);
        Mockito.verify(progressMessageService)
               .add(any());
    }

    @Test
    void testFailStepPhaseAbortIsInvoked() {
        prepareHistoricOperationsEventGetter(HistoricOperationEvent.EventType.STARTED, HistoricOperationEvent.EventType.FINISHED,
                                             HistoricOperationEvent.EventType.ABORT_EXECUTED);
        Exception exception = Assertions.assertThrows(SLException.class, () -> processStepHelper.failStepIfProcessIsAborted(context));
        Assertions.assertEquals(Messages.PROCESS_WAS_ABORTED, exception.getMessage());
    }

    @Test
    void testFailedStepPhaseAbortIsNotInvoked() {
        prepareHistoricOperationsEventGetter(HistoricOperationEvent.EventType.STARTED, HistoricOperationEvent.EventType.FINISHED);
        Assertions.assertDoesNotThrow(() -> processStepHelper.failStepIfProcessIsAborted(context));
    }

    private void prepareStepLogger() {
        ProcessLogger processLogger = Mockito.mock(ProcessLogger.class);
        Mockito.when(stepLogger.getProcessLogger())
               .thenReturn(processLogger);
    }

    private void prepareContext() {
        Mockito.when(context.getVariable(Variables.CORRELATION_ID))
               .thenReturn(CORRELATION_GUID);
        Mockito.when(context.getExecution())
               .thenReturn(execution);
    }

    private void prepareProgressMessagesService() {
        RuntimeService runtimeService = Mockito.mock(RuntimeService.class);
        ExecutionQuery executionQuery = Mockito.mock(ExecutionQuery.class);
        Mockito.when(processEngineConfiguration.getRuntimeService())
               .thenReturn(runtimeService);
        Mockito.when(runtimeService.createExecutionQuery())
               .thenReturn(executionQuery);
        Mockito.when(executionQuery.processInstanceId(any()))
               .thenReturn(executionQuery);
        List<Execution> mockedExecutions = getMockedExecutions();
        Mockito.when(executionQuery.list())
               .thenReturn(mockedExecutions);
    }

    private List<Execution> getMockedExecutions() {
        Execution execution = Mockito.mock(Execution.class);
        Mockito.when(execution.getActivityId())
               .thenReturn("activityId");
        return List.of(execution);
    }

    private void prepareHistoricOperationsEventGetter(HistoricOperationEvent.EventType... eventTypes) {
        List<HistoricOperationEvent> historicOperationEvents = mapEventTypesToHistoricEvents(eventTypes);
        Mockito.when(processHelper.getHistoricOperationEventByProcessId(CORRELATION_GUID))
               .thenReturn(historicOperationEvents);
    }

    private List<HistoricOperationEvent> mapEventTypesToHistoricEvents(HistoricOperationEvent.EventType... eventTypes) {
        return Arrays.stream(eventTypes)
                     .map(this::createImmutableHistoricOperationEvent)
                     .collect(Collectors.toList());
    }

    private ImmutableHistoricOperationEvent createImmutableHistoricOperationEvent(HistoricOperationEvent.EventType eventType) {
        return ImmutableHistoricOperationEvent.builder()
                                              .processId(CORRELATION_GUID)
                                              .type(eventType)
                                              .build();
    }
}

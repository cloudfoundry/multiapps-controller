package org.cloudfoundry.multiapps.controller.process.listeners;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableLoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.LogLevel;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.HistoricOperationEventService;
import org.cloudfoundry.multiapps.controller.core.cloudlogging.OperationLogsExporter;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerPersister;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.persistence.services.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.process.flowable.FlowableFacade;
import org.cloudfoundry.multiapps.controller.process.util.MockDelegateExecution;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.Execution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportCloudLoggingConfigurationListenerTest {

    private static final LoggingConfiguration LOGGING_CONFIGURATION = ImmutableLoggingConfiguration.builder()
                                                                                                   .operationId("op-1")
                                                                                                   .logLevel(LogLevel.INFO)
                                                                                                   .isFailSafe(true)
                                                                                                   .build();

    @Mock
    private ProgressMessageService progressMessageService;
    @Mock
    private ProcessLoggerProvider processLoggerProvider;
    @Mock
    private HistoricOperationEventService historicOperationEventService;
    @Mock
    private FlowableFacade flowableFacade;
    @Mock
    private ApplicationConfiguration configuration;
    @Mock
    private StepLogger.Factory stepLoggerFactory;
    @Mock
    private StepLogger stepLogger;
    @Mock
    private ProcessLoggerPersister processLoggerPersister;
    @Mock
    private OperationLogsExporter operationLogsExporter;

    @InjectMocks
    private ExportCloudLoggingConfigurationListener listener;

    private final DelegateExecution execution = MockDelegateExecution.createSpyInstance();

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        when(stepLoggerFactory.create(any(), any(), any(), any(), any())).thenReturn(stepLogger);
    }

    @Test
    void testNotifyInternal_withNoLoggingConfiguration_doesNothing() {
        listener.notify(execution);

        verify(flowableFacade, never()).setVariableInParentProcess(any(), anyString(), any());
        verify(flowableFacade, never()).setVariableInParentProcessUsingParentProcessInstanceId(any(), anyString(), any());
    }

    @Test
    void testNotifyInternal_withParentProcessInstanceId_setsVariableInParentProcessXSA() {
        VariableHandling.set(execution, Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION, LOGGING_CONFIGURATION);
        VariableHandling.set(execution, Variables.PARENT_PROCESS_INSTANCE_ID, "parent-process-1");

        listener.notify(execution);

        verify(flowableFacade).setVariableInParentProcessUsingParentProcessInstanceId(eq(execution),
                                                                                      eq(Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION.getName()),
                                                                                      any());
    }

    @Test
    void testNotifyInternal_withParentProcessInstanceId_setsSerializedValue() {
        VariableHandling.set(execution, Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION, LOGGING_CONFIGURATION);
        VariableHandling.set(execution, Variables.PARENT_PROCESS_INSTANCE_ID, "parent-process-1");

        listener.notify(execution);

        verify(flowableFacade).setVariableInParentProcessUsingParentProcessInstanceId(eq(execution),
                                                                                      eq(Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION.getName()),
                                                                                      anyString());
    }

    @Test
    void testNotifyInternal_withSuperExecution_setsVariableInParentProcess() {
        VariableHandling.set(execution, Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION, LOGGING_CONFIGURATION);
        prepareSuperExecution();

        listener.notify(execution);

        verify(flowableFacade).setVariableInParentProcess(eq(execution),
                                                          eq(Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION.getName()), any());
    }

    @Test
    void testNotifyInternal_withSuperExecution_setsSerializedValue() {
        VariableHandling.set(execution, Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION, LOGGING_CONFIGURATION);
        prepareSuperExecution();

        listener.notify(execution);

        verify(flowableFacade).setVariableInParentProcess(eq(execution),
                                                          eq(Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION.getName()),
                                                          anyString());
    }

    @Test
    void testNotifyInternal_withNoParentAndNoSuperExecution_setsVariableInCurrentExecution() {
        VariableHandling.set(execution, Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION, LOGGING_CONFIGURATION);

        listener.notify(execution);

        LoggingConfiguration result = VariableHandling.get(execution, Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION);
        assertEquals(LOGGING_CONFIGURATION, result);
    }

    @Test
    void testNotifyInternal_withNoParentAndNoSuperExecution_doesNotPropagateToParent() {
        VariableHandling.set(execution, Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION, LOGGING_CONFIGURATION);

        listener.notify(execution);

        verify(flowableFacade, never()).setVariableInParentProcess(any(), anyString(), any());
        verify(flowableFacade, never()).setVariableInParentProcessUsingParentProcessInstanceId(any(), anyString(), any());
    }

    @Test
    void testNotifyInternal_parentProcessInstanceIdTakesPrecedenceOverSuperExecution() {
        VariableHandling.set(execution, Variables.EXTERNAL_LOGGING_SERVICE_CONFIGURATION, LOGGING_CONFIGURATION);
        VariableHandling.set(execution, Variables.PARENT_PROCESS_INSTANCE_ID, "parent-process-1");
        prepareSuperExecution();

        listener.notify(execution);

        verify(flowableFacade).setVariableInParentProcessUsingParentProcessInstanceId(any(), anyString(), any());
        verify(flowableFacade, never()).setVariableInParentProcess(any(), anyString(), any());
    }

    private void prepareSuperExecution() {
        String parentId = "parent-execution-id";
        Mockito.doReturn(parentId)
               .when(execution)
               .getParentId();
        Execution parentExecution = Mockito.mock(Execution.class);
        when(parentExecution.getSuperExecutionId()).thenReturn("super-execution-id");
        when(flowableFacade.getParentExecution(parentId)).thenReturn(parentExecution);
    }
}

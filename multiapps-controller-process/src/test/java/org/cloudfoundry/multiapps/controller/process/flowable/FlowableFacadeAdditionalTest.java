package org.cloudfoundry.multiapps.controller.process.flowable;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ExecutionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.history.HistoricVariableInstanceQuery;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class FlowableFacadeAdditionalTest {

    private static final String PROCESS_INSTANCE_ID = "process-1";
    private static final String EXECUTION_ID = "execution-1";

    @Mock
    private ProcessEngine processEngine;
    @Mock
    private ProcessEngineConfiguration processEngineConfiguration;
    @Mock
    private DefaultAsyncJobExecutor asyncExecutor;
    @Mock
    private RuntimeService runtimeService;
    @Mock
    private ManagementService managementService;
    @Mock
    private HistoryService historyService;
    @Mock
    private ExecutionQuery executionQuery;
    @Mock
    private ProcessInstanceQuery processInstanceQuery;
    @Mock
    private HistoricProcessInstanceQuery historicProcessInstanceQuery;
    @Mock
    private HistoricVariableInstanceQuery historicVariableInstanceQuery;
    @Mock
    private HistoricActivityInstanceQuery historicActivityInstanceQuery;

    private FlowableFacade facade;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();

        Mockito.when(processEngine.getRuntimeService())
               .thenReturn(runtimeService);
        Mockito.when(processEngine.getManagementService())
               .thenReturn(managementService);
        Mockito.when(processEngine.getHistoryService())
               .thenReturn(historyService);
        Mockito.when(processEngine.getProcessEngineConfiguration())
               .thenReturn(processEngineConfiguration);
        Mockito.when(processEngineConfiguration.getAsyncExecutor())
               .thenReturn(asyncExecutor);

        facade = new FlowableFacade(processEngine);
    }

    @Test
    void testStartProcessDelegatesToRuntimeService() {
        ProcessInstance instance = Mockito.mock(ProcessInstance.class);
        Map<String, Object> variables = Map.of("k", "v");
        Mockito.when(runtimeService.startProcessInstanceByKey("deploy", variables))
               .thenReturn(instance);

        Assertions.assertSame(instance, facade.startProcess("deploy", variables));
    }

    @Test
    void testGetProcessInstanceIdReadsCorrelationIdVariable() {
        VariableInstance variableInstance = Mockito.mock(VariableInstance.class);
        Mockito.when(variableInstance.getTextValue())
               .thenReturn("correlation-1");
        Mockito.when(runtimeService.getVariableInstance(EXECUTION_ID, Constants.CORRELATION_ID))
               .thenReturn(variableInstance);

        Assertions.assertEquals("correlation-1", facade.getProcessInstanceId(EXECUTION_ID));
    }

    @Test
    void testGetCurrentTaskIdReturnsNullWhenAbsentInRuntimeAndHistory() {
        Mockito.when(runtimeService.getVariableInstance(ArgumentMatchers.eq(EXECUTION_ID), ArgumentMatchers.eq(Constants.TASK_ID)))
               .thenReturn(null);
        mockHistoricVariableInstanceQuery(null);

        Assertions.assertNull(facade.getCurrentTaskId(EXECUTION_ID));
    }

    @Test
    void testGetSubprocessInstanceIdFallsBackToHistoricVariable() {
        Mockito.when(runtimeService.getVariableInstance(ArgumentMatchers.eq(EXECUTION_ID), ArgumentMatchers.anyString()))
               .thenReturn(null);
        HistoricVariableInstance historicInstance = Mockito.mock(HistoricVariableInstance.class);
        Mockito.when(historicInstance.getValue())
               .thenReturn("subprocess-1");
        mockHistoricVariableInstanceQuery(historicInstance);

        Assertions.assertEquals("subprocess-1", facade.getSubprocessInstanceId(EXECUTION_ID));
    }

    @Test
    void testGetProcessInstanceQueriesByProcessInstanceId() {
        ProcessInstance instance = Mockito.mock(ProcessInstance.class);
        Mockito.when(runtimeService.createProcessInstanceQuery())
               .thenReturn(processInstanceQuery);
        Mockito.when(processInstanceQuery.processInstanceId(PROCESS_INSTANCE_ID))
               .thenReturn(processInstanceQuery);
        Mockito.when(processInstanceQuery.singleResult())
               .thenReturn(instance);

        Assertions.assertSame(instance, facade.getProcessInstance(PROCESS_INSTANCE_ID));
    }

    @Test
    void testHasDeadLetterJobsReturnsFalseWhenNoExecutions() {
        mockExecutionQueryReturning(List.of());

        Assertions.assertFalse(facade.hasDeadLetterJobs(PROCESS_INSTANCE_ID));
    }

    @Test
    void testGetHistoricSubProcessIdsExcludesCorrelationId() {
        HistoricVariableInstance v1 = Mockito.mock(HistoricVariableInstance.class);
        Mockito.when(v1.getProcessInstanceId())
               .thenReturn("correlation-1");
        HistoricVariableInstance v2 = Mockito.mock(HistoricVariableInstance.class);
        Mockito.when(v2.getProcessInstanceId())
               .thenReturn("subprocess-1");
        Mockito.when(historyService.createHistoricVariableInstanceQuery())
               .thenReturn(historicVariableInstanceQuery);
        Mockito.when(historicVariableInstanceQuery.variableValueEquals(Constants.CORRELATION_ID, "correlation-1"))
               .thenReturn(historicVariableInstanceQuery);
        Mockito.when(historicVariableInstanceQuery.orderByProcessInstanceId())
               .thenReturn(historicVariableInstanceQuery);
        Mockito.when(historicVariableInstanceQuery.asc())
               .thenReturn(historicVariableInstanceQuery);
        Mockito.when(historicVariableInstanceQuery.list())
               .thenReturn(List.of(v1, v2));

        List<String> result = facade.getHistoricSubProcessIds("correlation-1");

        Assertions.assertEquals(List.of("subprocess-1"), result);
    }

    @Test
    void testGetHistoricProcessByIdQueriesHistoryService() {
        HistoricProcessInstance instance = Mockito.mock(HistoricProcessInstance.class);
        Mockito.when(historyService.createHistoricProcessInstanceQuery())
               .thenReturn(historicProcessInstanceQuery);
        Mockito.when(historicProcessInstanceQuery.processInstanceId(PROCESS_INSTANCE_ID))
               .thenReturn(historicProcessInstanceQuery);
        Mockito.when(historicProcessInstanceQuery.singleResult())
               .thenReturn(instance);

        Assertions.assertSame(instance, facade.getHistoricProcessById(PROCESS_INSTANCE_ID));
    }

    @Test
    void testGetHistoricVariableInstanceQueriesByProcessAndVariableName() {
        HistoricVariableInstance variable = Mockito.mock(HistoricVariableInstance.class);
        Mockito.when(historyService.createHistoricVariableInstanceQuery())
               .thenReturn(historicVariableInstanceQuery);
        Mockito.when(historicVariableInstanceQuery.processInstanceId(PROCESS_INSTANCE_ID))
               .thenReturn(historicVariableInstanceQuery);
        Mockito.when(historicVariableInstanceQuery.variableName("foo"))
               .thenReturn(historicVariableInstanceQuery);
        Mockito.when(historicVariableInstanceQuery.singleResult())
               .thenReturn(variable);

        Assertions.assertSame(variable, facade.getHistoricVariableInstance(PROCESS_INSTANCE_ID, "foo"));
    }

    @Test
    void testTriggerForwardsToRuntimeService() {
        Map<String, Object> vars = Map.of("k", "v");

        facade.trigger(EXECUTION_ID, vars);

        Mockito.verify(runtimeService)
               .trigger(EXECUTION_ID, vars);
    }

    @Test
    void testDeleteProcessInstanceCallsRuntimeServiceOnce() {
        facade.deleteProcessInstance(PROCESS_INSTANCE_ID, "user requested abort");

        Mockito.verify(runtimeService)
               .deleteProcessInstance(PROCESS_INSTANCE_ID, "user requested abort");
    }

    @Test
    void testDeleteProcessInstanceRetriesAfterOptimisticLockingException() {
        Mockito.doThrow(new FlowableOptimisticLockingException("conflict"))
               .doNothing()
               .when(runtimeService)
               .deleteProcessInstance(PROCESS_INSTANCE_ID, "abort");

        facade.deleteProcessInstance(PROCESS_INSTANCE_ID, "abort");

        Mockito.verify(runtimeService, Mockito.times(2))
               .deleteProcessInstance(PROCESS_INSTANCE_ID, "abort");
    }

    @Test
    void testDeleteProcessInstanceThrowsAfterDeadlinePastWithOptimisticLocking() {
        Mockito.doThrow(new FlowableOptimisticLockingException("conflict"))
               .when(runtimeService)
               .deleteProcessInstance(PROCESS_INSTANCE_ID, "abort");
        FlowableFacade facadeWithImmediateDeadline = new FlowableFacade(processEngine) {
            @Override
            protected boolean isPastDeadline(long deadline) {
                return true;
            }
        };

        Assertions.assertThrows(IllegalStateException.class,
                                () -> facadeWithImmediateDeadline.deleteProcessInstance(PROCESS_INSTANCE_ID, "abort"));
    }

    @Test
    void testIsProcessInstanceAtReceiveTaskReturnsFalseWhenNoExecutions() {
        mockExecutionQueryReturning(List.of());

        Assertions.assertFalse(facade.isProcessInstanceAtReceiveTask(PROCESS_INSTANCE_ID));
    }

    @Test
    void testFindExecutionsAtReceiveTaskFiltersByActiveActivity() {
        Execution active = Mockito.mock(Execution.class);
        Mockito.when(active.getActivityId())
               .thenReturn("act-1");
        Mockito.when(active.getId())
               .thenReturn("exec-active");
        Execution inactive = Mockito.mock(Execution.class);
        Mockito.when(inactive.getActivityId())
               .thenReturn(null);
        mockExecutionQueryReturning(List.of(active, inactive));

        Mockito.when(historyService.createHistoricActivityInstanceQuery())
               .thenReturn(historicActivityInstanceQuery);
        Mockito.when(historicActivityInstanceQuery.activityId("act-1"))
               .thenReturn(historicActivityInstanceQuery);
        Mockito.when(historicActivityInstanceQuery.executionId("exec-active"))
               .thenReturn(historicActivityInstanceQuery);
        Mockito.when(historicActivityInstanceQuery.activityType("receiveTask"))
               .thenReturn(historicActivityInstanceQuery);
        HistoricActivityInstance receivedActivity = Mockito.mock(HistoricActivityInstance.class);
        Mockito.when(historicActivityInstanceQuery.list())
               .thenReturn(List.of(receivedActivity));

        List<Execution> result = facade.findExecutionsAtReceiveTask(PROCESS_INSTANCE_ID);

        Assertions.assertEquals(List.of(active), result);
    }

    @Test
    void testGetActiveProcessExecutionsFiltersOutNullActivityId() {
        Execution active = Mockito.mock(Execution.class);
        Mockito.when(active.getActivityId())
               .thenReturn("act-1");
        Execution inactive = Mockito.mock(Execution.class);
        Mockito.when(inactive.getActivityId())
               .thenReturn(null);
        mockExecutionQueryReturning(List.of(active, inactive));

        List<Execution> result = facade.getActiveProcessExecutions(PROCESS_INSTANCE_ID);

        Assertions.assertEquals(List.of(active), result);
    }

    @Test
    void testSuspendProcessInstanceForwardsToRuntimeService() {
        facade.suspendProcessInstance(PROCESS_INSTANCE_ID);

        Mockito.verify(runtimeService)
               .suspendProcessInstanceById(PROCESS_INSTANCE_ID);
    }

    @Test
    void testIsJobExecutorActiveReadsAsyncExecutor() {
        Mockito.when(asyncExecutor.isActive())
               .thenReturn(true);

        Assertions.assertTrue(facade.isJobExecutorActive());
    }

    @Test
    void testGetProcessEngineReturnsConstructorArgument() {
        Assertions.assertSame(processEngine, facade.getProcessEngine());
    }

    @Test
    void testFindAllRunningProcessInstanceStartedBeforeDelegatesToProcessInstanceQuery() {
        LocalDateTime before = LocalDateTime.parse("2026-05-01T10:00:00");
        Mockito.when(runtimeService.createProcessInstanceQuery())
               .thenReturn(processInstanceQuery);
        Mockito.when(processInstanceQuery.excludeSubprocesses(true))
               .thenReturn(processInstanceQuery);
        Mockito.when(processInstanceQuery.startedBefore(ArgumentMatchers.any(Date.class)))
               .thenReturn(processInstanceQuery);
        ProcessInstance instance = Mockito.mock(ProcessInstance.class);
        Mockito.when(processInstanceQuery.list())
               .thenReturn(List.of(instance));

        Assertions.assertEquals(List.of(instance), facade.findAllRunningProcessInstanceStartedBefore(before));
    }

    @Test
    void testGetParentExecutionReturnsSingleResult() {
        Execution parent = Mockito.mock(Execution.class);
        Mockito.when(runtimeService.createExecutionQuery())
               .thenReturn(executionQuery);
        Mockito.when(executionQuery.executionId("parent-id"))
               .thenReturn(executionQuery);
        Mockito.when(executionQuery.singleResult())
               .thenReturn(parent);

        Assertions.assertSame(parent, facade.getParentExecution("parent-id"));
    }

    @Test
    void testSetVariableInParentProcessUsesParentExecutionsSuperExecutionId() {
        DelegateExecution childExecution = Mockito.mock(DelegateExecution.class);
        Mockito.when(childExecution.getParentId())
               .thenReturn("parent-id");

        Execution parent = Mockito.mock(Execution.class);
        Mockito.when(parent.getSuperExecutionId())
               .thenReturn("super-id");
        Mockito.when(runtimeService.createExecutionQuery())
               .thenReturn(executionQuery);
        Mockito.when(executionQuery.executionId("parent-id"))
               .thenReturn(executionQuery);
        Mockito.when(executionQuery.singleResult())
               .thenReturn(parent);

        facade.setVariableInParentProcess(childExecution, "varName", "varValue");

        Mockito.verify(runtimeService)
               .setVariable("super-id", "varName", "varValue");
    }

    private void mockExecutionQueryReturning(List<Execution> executions) {
        Mockito.when(runtimeService.createExecutionQuery())
               .thenReturn(executionQuery);
        Mockito.when(executionQuery.rootProcessInstanceId(ArgumentMatchers.anyString()))
               .thenReturn(executionQuery);
        Mockito.when(executionQuery.list())
               .thenReturn(executions);
    }

    private void mockHistoricVariableInstanceQuery(HistoricVariableInstance result) {
        Mockito.when(historyService.createHistoricVariableInstanceQuery())
               .thenReturn(historicVariableInstanceQuery);
        Mockito.when(historicVariableInstanceQuery.executionId(ArgumentMatchers.anyString()))
               .thenReturn(historicVariableInstanceQuery);
        Mockito.when(historicVariableInstanceQuery.variableName(ArgumentMatchers.anyString()))
               .thenReturn(historicVariableInstanceQuery);
        Mockito.when(historicVariableInstanceQuery.singleResult())
               .thenReturn(result);
    }
}

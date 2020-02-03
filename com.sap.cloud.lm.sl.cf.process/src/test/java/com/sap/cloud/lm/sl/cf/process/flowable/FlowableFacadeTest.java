package com.sap.cloud.lm.sl.cf.process.flowable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.flowable.common.engine.impl.db.SuspensionState;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricActivityInstanceQuery;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ExecutionQuery;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.job.api.DeadLetterJobQuery;
import org.flowable.job.api.Job;
import org.flowable.job.api.TimerJobQuery;
import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

class FlowableFacadeTest {

    private static final String ROOT_PROCESS_INSTANCE_ID = "35640629-619d-4e8e-9840-6bca38f51f35";
    private static final String PROCESS_INSTANCE_ID = "65640629-619d-4e8e-9840-6bca38f51f35";
    private static final String PARENT_EXECUTION_ID = "05640649-619d-4e8e-9840-6bca38f51f35";
    private static final String EXECUTION_ID = "89640629-619d-4e8e-9840-6bca38f51f35";
    private static final String DELETE_REASON = "Process was aborted";
    private static final String DEPLOY_MODULES_PARALLEL = "deployModulesParallel";
    private FlowableFacade flowableFacade;

    @Mock
    private DefaultAsyncJobExecutor mockedAsyncExecutor;

    @Mock
    private ProcessEngine mockedProcessEngine;

    @Mock
    private RuntimeService mockedRuntimeService;

    @Mock
    private ExecutionQuery mockedExecutionQuery;

    @Mock
    private HistoryService mockedHistoryService;

    @Mock
    private HistoricActivityInstanceQuery mockedHistoricActivityInstanceQuery;

    @Mock
    private ProcessInstanceQuery mockedProcessInstanceQuery;

    @Mock
    private ProcessEngineConfiguration mockedProcessEngineConfiguration;

    @Mock
    private ManagementService mockedManagementService;

    @Mock
    private TimerJobQuery mockedTimerJobQuery;

    @Mock
    private DeadLetterJobQuery mockedDeadLetterJobQuery;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        Mockito.when(mockedProcessEngineConfiguration.getAsyncExecutor())
               .thenReturn(mockedAsyncExecutor);
        Mockito.when(mockedProcessEngine.getProcessEngineConfiguration())
               .thenReturn(mockedProcessEngineConfiguration);
        Mockito.when(mockedProcessEngine.getRuntimeService())
               .thenReturn(mockedRuntimeService);
        Mockito.when(mockedRuntimeService.createExecutionQuery())
               .thenReturn(mockedExecutionQuery);
        Mockito.when(mockedProcessEngine.getHistoryService())
               .thenReturn(mockedHistoryService);
        Mockito.when(mockedHistoryService.createHistoricActivityInstanceQuery())
               .thenReturn(mockedHistoricActivityInstanceQuery);
        Mockito.when(mockedRuntimeService.createExecutionQuery())
               .thenReturn(mockedExecutionQuery);
        Mockito.when(mockedRuntimeService.createProcessInstanceQuery())
               .thenReturn(mockedProcessInstanceQuery);
        Mockito.when(mockedProcessEngine.getManagementService())
               .thenReturn(mockedManagementService);
        Mockito.when(mockedManagementService.createTimerJobQuery())
               .thenReturn(mockedTimerJobQuery);
        Mockito.when(mockedManagementService.createDeadLetterJobQuery())
               .thenReturn(mockedDeadLetterJobQuery);

        flowableFacade = new FlowableFacade(mockedProcessEngine);
    }

    @Test
    public void testAsyncExecutorMethodsAreCalled() {
        flowableFacade.shutdownJobExecutor();
        Mockito.verify(mockedAsyncExecutor, times(1))
               .shutdown();
    }

    @Test
    public void testDeleteProcessWhenAtReceiveTask() {
        mockExecutionQueryListExecutions(getExecutionsWithValidActivityIds());
        mockHistoricActivityInstanceQuery(getMockedHistoryActivityInstances());
        flowableFacade.deleteProcessInstance(ROOT_PROCESS_INSTANCE_ID, DELETE_REASON);
        verifyProcessIsDeleted();
        verifyProcessIsNotSuspended();
    }

    @Test
    public void testDeleteProcessWhenProcessHasBeenAlreadyDeleted() {
        mockExecutionQueryListExecutions(getExecutionsWithValidActivityIds());
        mockExecutionQuery();
        mockHistoricActivityInstanceQuery(Collections.emptyList());
        Mockito.when(mockedTimerJobQuery.executionId(EXECUTION_ID))
               .thenReturn(mockedTimerJobQuery);
        Mockito.when(mockedDeadLetterJobQuery.processInstanceId(EXECUTION_ID))
               .thenReturn(mockedDeadLetterJobQuery);
        Mockito.when(mockedProcessInstanceQuery.processInstanceId(ROOT_PROCESS_INSTANCE_ID))
               .thenReturn(mockedProcessInstanceQuery);
        flowableFacade.deleteProcessInstance(ROOT_PROCESS_INSTANCE_ID, DELETE_REASON);
        verifyProcessIsNotDeleted();
        verifyProcessIsNotSuspended();
    }

    @Test
    public void testDeleteProcessInstanceWhenThereAreNoActiveExecutions() {
        mockExecutionQueryListExecutions(Collections.emptyList());
        mockExecutionQuery();
        mockHistoricActivityInstanceQuery(Collections.emptyList());
        flowableFacade.deleteProcessInstance(ROOT_PROCESS_INSTANCE_ID, DELETE_REASON);
        verifyProcessIsDeleted();
        verifyProcessIsSuspended();
    }

    @Test
    public void testDeleteProcessInstanceWhenAllActiveExecutionsHaveDeadLetterJobs() {
        mockExecutionQueryListExecutions(getExecutionsWithValidActivityIds());
        mockExecutionQuery();
        mockHistoricActivityInstanceQuery(Collections.emptyList());
        mockProcessFound();
        Mockito.when(mockedTimerJobQuery.executionId(EXECUTION_ID))
               .thenReturn(mockedTimerJobQuery);
        Mockito.when(mockedDeadLetterJobQuery.processInstanceId(EXECUTION_ID))
               .thenReturn(mockedDeadLetterJobQuery);
        Mockito.when(mockedDeadLetterJobQuery.list())
               .thenReturn(getMockedDeadLetterJobs());
        Mockito.when(mockedProcessInstanceQuery.processInstanceId(ROOT_PROCESS_INSTANCE_ID))
               .thenReturn(mockedProcessInstanceQuery);
        flowableFacade.deleteProcessInstance(ROOT_PROCESS_INSTANCE_ID, DELETE_REASON);
        verifyProcessIsDeleted();
        verifyProcessIsSuspended();
    }

    @Test
    public void testDeleteProcessInstanceWhichHasBeenSuspended() {
        mockExecutionQueryListExecutions(getExecutionsWithValidActivityIds());
        mockExecutionQuery();
        mockProcessFound();
        mockHistoricActivityInstanceQuery(Collections.emptyList());
        Mockito.when(mockedTimerJobQuery.executionId(EXECUTION_ID))
               .thenReturn(mockedTimerJobQuery);
        Mockito.when(mockedDeadLetterJobQuery.processInstanceId(EXECUTION_ID))
               .thenReturn(mockedDeadLetterJobQuery);
        Mockito.when(mockedDeadLetterJobQuery.list())
               .thenReturn(getMockedDeadLetterJobs());
        Mockito.when(mockedProcessInstanceQuery.processInstanceId(ROOT_PROCESS_INSTANCE_ID))
               .thenReturn(mockedProcessInstanceQuery);
        Mockito.when(mockedExecutionQuery.singleResult())
               .thenReturn(getSuspendedExecution());
        flowableFacade.deleteProcessInstance(ROOT_PROCESS_INSTANCE_ID, DELETE_REASON);
        verifyProcessIsDeleted();
        verifyProcessIsNotSuspended();
    }

    @Test
    public void testGetInactiveExecutionIdsWithoutChildren() {
        mockExecutionQueryListExecutions(getParentAndChildrenExecution());
        List<Execution> inactiveExecutionIdsWithoutChildren = flowableFacade.getLeafExecutionsByFilter(ROOT_PROCESS_INSTANCE_ID,
                                                                                                         executionEntity -> !executionEntity.isActive());
        Assertions.assertEquals(1, inactiveExecutionIdsWithoutChildren.size());
        Assertions.assertEquals(PROCESS_INSTANCE_ID, inactiveExecutionIdsWithoutChildren.get(0)
                                                                                        .getProcessInstanceId());
    }

    private List<Execution> getExecutionsWithValidActivityIds() {
        ExecutionEntityImpl executionWithValidActivityId = Mockito.mock(ExecutionEntityImpl.class);
        Mockito.when(executionWithValidActivityId.getActivityId())
               .thenReturn(DEPLOY_MODULES_PARALLEL);
        Mockito.when(executionWithValidActivityId.getId())
               .thenReturn(EXECUTION_ID);
        Mockito.when(executionWithValidActivityId.isActive())
               .thenReturn(true);
        Mockito.when(executionWithValidActivityId.getProcessInstanceId())
               .thenReturn(EXECUTION_ID);
        Execution executionWithInvalidActivityId = Mockito.mock(ExecutionEntityImpl.class);
        return Arrays.asList(executionWithValidActivityId, executionWithInvalidActivityId);
    }

    private void mockExecutionQueryListExecutions(List<Execution> executions) {
        Mockito.when(mockedExecutionQuery.rootProcessInstanceId(ROOT_PROCESS_INSTANCE_ID))
               .thenReturn(mockedExecutionQuery);
        Mockito.when(mockedExecutionQuery.list())
               .thenReturn(executions);
    }

    private void mockHistoricActivityInstanceQuery(List<HistoricActivityInstance> historicActivityInstances) {
        Mockito.when(mockedHistoricActivityInstanceQuery.activityId(DEPLOY_MODULES_PARALLEL))
               .thenReturn(mockedHistoricActivityInstanceQuery);
        Mockito.when(mockedHistoricActivityInstanceQuery.executionId(EXECUTION_ID))
               .thenReturn(mockedHistoricActivityInstanceQuery);
        Mockito.when(mockedHistoricActivityInstanceQuery.activityType("receiveTask"))
               .thenReturn(mockedHistoricActivityInstanceQuery);
        Mockito.when(mockedHistoricActivityInstanceQuery.list())
               .thenReturn(historicActivityInstances);
    }

    private List<HistoricActivityInstance> getMockedHistoryActivityInstances() {
        HistoricActivityInstance historicActivityInstance = Mockito.mock(HistoricActivityInstance.class);
        return Collections.singletonList(historicActivityInstance);
    }

    private void mockExecutionQuery() {
        Mockito.when(mockedExecutionQuery.processInstanceId(ROOT_PROCESS_INSTANCE_ID))
               .thenReturn(mockedExecutionQuery);
        Mockito.when(mockedExecutionQuery.executionId(ROOT_PROCESS_INSTANCE_ID))
               .thenReturn(mockedExecutionQuery);
    }

    private void mockProcessFound() {
        Mockito.when(mockedProcessInstanceQuery.processInstanceId(ROOT_PROCESS_INSTANCE_ID))
               .thenReturn(mockedProcessInstanceQuery);
        Mockito.when(mockedProcessInstanceQuery.singleResult())
               .thenReturn(Mockito.mock(ProcessInstance.class));
    }

    private List<Job> getMockedDeadLetterJobs() {
        Job deadLetterJob = Mockito.mock(Job.class);
        return Collections.singletonList(deadLetterJob);
    }

    private void verifyProcessIsNotDeleted() {
        Mockito.verify(mockedProcessEngine.getRuntimeService(), never())
               .deleteProcessInstance(any(), anyString());
    }

    private void verifyProcessIsNotSuspended() {
        Mockito.verify(mockedProcessEngine.getRuntimeService(), never())
               .suspendProcessInstanceById(any());
    }

    private void verifyProcessIsDeleted() {
        Mockito.verify(mockedProcessEngine.getRuntimeService(), times(1))
               .deleteProcessInstance(ROOT_PROCESS_INSTANCE_ID, DELETE_REASON);
    }

    private void verifyProcessIsSuspended() {
        Mockito.verify(mockedProcessEngine.getRuntimeService(), times(1))
               .suspendProcessInstanceById(ROOT_PROCESS_INSTANCE_ID);
    }

    private Execution getSuspendedExecution() {
        ExecutionEntityImpl execution = new ExecutionEntityImpl();
        execution.setSuspensionState(SuspensionState.SUSPENDED.getStateCode());
        return execution;
    }

    private List<Execution> getParentAndChildrenExecution() {
        ExecutionEntityImpl childExecution = Mockito.mock(ExecutionEntityImpl.class);
        Mockito.when(childExecution.getParentId())
               .thenReturn(PARENT_EXECUTION_ID);
        Mockito.when(childExecution.getId())
               .thenReturn(EXECUTION_ID);
        Mockito.when(childExecution.getProcessInstanceId())
               .thenReturn(PROCESS_INSTANCE_ID);
        ExecutionEntityImpl parentExecution = Mockito.mock(ExecutionEntityImpl.class);
        Mockito.when(parentExecution.getId())
               .thenReturn(PARENT_EXECUTION_ID);
        Mockito.when(parentExecution.getProcessInstanceId())
               .thenReturn(PARENT_EXECUTION_ID);
        return Arrays.asList(parentExecution, childExecution);
    }
}

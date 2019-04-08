package com.sap.cloud.lm.sl.cf.core.flowable;

import static java.text.MessageFormat.format;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.persistence.message.Constants;
import com.sap.cloud.lm.sl.cf.web.api.model.State;

@Component
public class FlowableFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableFacade.class);

    private static final int DEFAULT_JOB_RETRIES = 0;
    private static final int DEFAULT_ABORT_TIMEOUT_MS = 30000;

    private final ProcessEngine processEngine;

    @Inject
    public FlowableFacade(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    public ProcessInstance startProcess(String userId, String processDefinitionKey, Map<String, Object> variables) {
        try {
            processEngine.getIdentityService()
                .setAuthenticatedUserId(userId);
            return processEngine.getRuntimeService()
                .startProcessInstanceByKey(processDefinitionKey, variables);
        } finally {
            // After the setAuthenticatedUserId() method is invoked, all Flowable service methods
            // executed within the current thread will have access to this userId. Just before
            // leaving the method, the userId is set to null, preventing other services from using
            // it unintentionally.
            processEngine.getIdentityService()
                .setAuthenticatedUserId(null);
        }
    }

    public State getProcessInstanceState(String processInstanceId) {
        ProcessInstance processInstance = getProcessInstance(processInstanceId);
        if (processInstance != null) {
            return getActiveProcessState(processInstance);
        }

        return getInactiveProcessState(processInstanceId);
    }

    private State getInactiveProcessState(String processInstanceId) {
        if (hasDeleteReason(processInstanceId)) {
            return State.ABORTED;
        }

        return State.FINISHED;
    }

    private State getActiveProcessState(ProcessInstance processInstance) {
        String processInstanceId = processInstance.getProcessInstanceId();
        if (isProcessInstanceAtReceiveTask(processInstanceId)) {
            return State.ACTION_REQUIRED;
        }

        if (hasDeadLetterJobs(processInstanceId)) {
            return State.ERROR;
        }

        return State.RUNNING;
    }

    private boolean hasDeleteReason(String processId) {
        HistoricProcessInstance historicProcessInstance = processEngine.getHistoryService()
            .createHistoricProcessInstanceQuery()
            .processInstanceId(processId)
            .singleResult();
        return historicProcessInstance != null && processHierarchyHasDeleteReason(historicProcessInstance);
    }

    public ProcessInstance getProcessInstance(String processId) {
        return processEngine.getRuntimeService()
            .createProcessInstanceQuery()
            .processInstanceId(processId)
            .singleResult();
    }

    private boolean processHierarchyHasDeleteReason(HistoricProcessInstance historicProcessInstance) {
        if (historicProcessInstance.getDeleteReason() != null) {
            return true;
        }

        List<HistoricProcessInstance> children = processEngine.getHistoryService()
            .createHistoricProcessInstanceQuery()
            .superProcessInstanceId(historicProcessInstance.getId())
            .list();
        return children.stream()
            .anyMatch(this::processHierarchyHasDeleteReason);
    }

    private boolean hasDeadLetterJobs(String processId) {
        return !getDeadLetterJobs(processId).isEmpty();
    }

    private List<Job> getDeadLetterJobs(String processId) {
        List<Execution> allProcessExecutions = getAllProcessExecutions(processId);
        return allProcessExecutions.stream()
            .map(this::getDeadLetterJobsForExecution)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    private List<Job> getDeadLetterJobsForExecution(Execution execution) {
        return processEngine.getManagementService()
            .createDeadLetterJobQuery()
            .processInstanceId(execution.getProcessInstanceId())
            .list();
    }

    public List<String> getHistoricSubProcessIds(String correlationId) {
        return retrieveVariablesByCorrelationId(correlationId).stream()
            .map(HistoricVariableInstance::getProcessInstanceId)
            .filter(id -> !id.equals(correlationId))
            .collect(Collectors.toList());
    }

    private List<HistoricVariableInstance> retrieveVariablesByCorrelationId(String correlationId) {
        return processEngine.getHistoryService()
            .createHistoricVariableInstanceQuery()
            .variableValueEquals(Constants.CORRELATION_ID, correlationId)
            .orderByProcessInstanceId()
            .asc()
            .list();
    }

    public HistoricVariableInstance getHistoricVariableInstance(String processInstanceId, String variableName) {
        return processEngine.getHistoryService()
            .createHistoricVariableInstanceQuery()
            .processInstanceId(processInstanceId)
            .variableName(variableName)
            .singleResult();
    }

    public List<String> getActiveHistoricSubProcessIds(String correlationId) {
        return getHistoricSubProcessIds(correlationId).stream()
            .filter(this::isActive)
            .collect(Collectors.toList());
    }

    private boolean isActive(String processId) {
        return getHistoricActivityInstance(processId, "endEvent") == null;
    }

    private HistoricActivityInstance getHistoricActivityInstance(String processId, String activityType) {
        return processEngine.getHistoryService()
            .createHistoricActivityInstanceQuery()
            .activityType(activityType)
            .processInstanceId(processId)
            .singleResult();
    }

    public String getActivityType(String processInstanceId, String executionId, String activityId) {
        List<HistoricActivityInstance> historicInstancesList = processEngine.getHistoryService()
            .createHistoricActivityInstanceQuery()
            .processInstanceId(processInstanceId)
            .activityId(activityId)
            .executionId(executionId)
            .orderByHistoricActivityInstanceEndTime()
            .desc()
            .list();
        return !historicInstancesList.isEmpty() ? historicInstancesList.get(0)
            .getActivityType() : null;
    }

    public Execution getProcessExecution(String processInstanceId) {
        return getExecutionsByProcessId(processInstanceId).stream()
            .filter(execution -> execution.getActivityId() != null)
            .findFirst()
            .orElse(null);
    }

    private List<Execution> getExecutionsByProcessId(String processInstanceId) {
        return processEngine.getRuntimeService()
            .createExecutionQuery()
            .rootProcessInstanceId(processInstanceId)
            .list();
    }

    public void executeJob(String userId, String processInstanceId) {
        List<Job> deadLetterJobs = getDeadLetterJobs(processInstanceId);
        if (deadLetterJobs.isEmpty()) {
            LOGGER.info(MessageFormat.format("No dead letter jobs found for process with id {0}", processInstanceId));
            return;
        }
        moveDeadLetterJobsToExecutableJobs(userId, deadLetterJobs);
    }

    private void moveDeadLetterJobsToExecutableJobs(String userId, List<Job> deadLetterJobs) {
        for (Job deadLetterJob : deadLetterJobs) {
            try {
                processEngine.getIdentityService()
                    .setAuthenticatedUserId(userId);
                moveDeadLetterJobToExecutableJob(deadLetterJob);
            } finally {
                processEngine.getIdentityService()
                    .setAuthenticatedUserId(null);
            }
        }
    }

    private void moveDeadLetterJobToExecutableJob(Job deadLetterJob) {
        processEngine.getManagementService()
            .moveDeadLetterJobToExecutableJob(deadLetterJob.getId(), DEFAULT_JOB_RETRIES);
    }

    public void trigger(String userId, String executionId) {
        try {
            processEngine.getIdentityService()
                .setAuthenticatedUserId(userId);
            processEngine.getRuntimeService()
                .trigger(executionId);
        } finally {
            processEngine.getIdentityService()
                .setAuthenticatedUserId(null);
        }
    }

    public void deleteProcessInstance(String userId, String processInstanceId, String deleteReason) {
        try {
            processEngine.getIdentityService()
                .setAuthenticatedUserId(userId);

            long deadline = System.currentTimeMillis() + DEFAULT_ABORT_TIMEOUT_MS;
            while (true) {
                try {
                    LOGGER.debug(format(Messages.SETTING_VARIABLE, Constants.PROCESS_ABORTED, Boolean.TRUE));

                    // TODO: Use execution ID instead of process instance ID, as they can be
                    // different if the process has parallel executions.
                    processEngine.getRuntimeService()
                        .setVariable(processInstanceId, Constants.PROCESS_ABORTED, Boolean.TRUE);
                    processEngine.getRuntimeService()
                        .deleteProcessInstance(processInstanceId, deleteReason);
                    break;
                } catch (FlowableOptimisticLockingException e) {
                    if (isPastDeadline(deadline)) {
                        throw new IllegalStateException(Messages.ABORT_OPERATION_TIMED_OUT, e);
                    }
                    LOGGER.warn(format(Messages.RETRYING_PROCESS_ABORT, processInstanceId));
                }
            }
        } finally {
            processEngine.getIdentityService()
                .setAuthenticatedUserId(null);
        }
    }

    protected boolean isPastDeadline(long deadline) {
        return System.currentTimeMillis() >= deadline;
    }

    public boolean isProcessInstanceAtReceiveTask(String processInstanceId) {
        List<Execution> executionsAtReceiveTask = findExecutionsAtReceiveTask(processInstanceId);
        return !executionsAtReceiveTask.isEmpty();
    }

    public List<Execution> findExecutionsAtReceiveTask(String processInstanceId) {
        List<Execution> allProcessExecutions = getActiveProcessExecutions(processInstanceId);

        return allProcessExecutions.stream()
            .filter(execution -> !findCurrentActivitiesAtReceiveTask(execution).isEmpty())
            .collect(Collectors.toList());

    }

    public List<Execution> getActiveProcessExecutions(String processInstanceId) {
        List<Execution> allProcessExecutions = getAllProcessExecutions(processInstanceId);

        return allProcessExecutions.stream()
            .filter(e -> e.getActivityId() != null)
            .collect(Collectors.toList());
    }

    private List<Execution> getAllProcessExecutions(String processInstanceId) {
        return processEngine.getRuntimeService()
            .createExecutionQuery()
            .rootProcessInstanceId(processInstanceId)
            .list();
    }

    private List<HistoricActivityInstance> findCurrentActivitiesAtReceiveTask(Execution execution) {
        return processEngine.getHistoryService()
            .createHistoricActivityInstanceQuery()
            .activityId(execution.getActivityId())
            .executionId(execution.getId())
            .activityType("receiveTask")
            .list();
    }

    public void activateProcessInstance(String processInstanceId) {
        processEngine.getRuntimeService()
            .activateProcessInstanceById(processInstanceId);
    }

    public void suspendProcessInstance(String processInstanceId) {
        processEngine.getRuntimeService()
            .suspendProcessInstanceById(processInstanceId);
    }

    public boolean isProcessInstanceSuspended(String processInstanceId) {
        ProcessInstance processInstance = getProcessInstance(processInstanceId);
        return processInstance != null && processInstance.isSuspended();
    }

    public void shutdownJobExecutor(long secondsToWaitOnShutdown) {
        setSecondsToWaitOnJobExecutorShutdown(secondsToWaitOnShutdown);
        LOGGER.info(Messages.SHUTTING_DOWN_FLOWABLE_JOB_EXECUTOR);
        AsyncExecutor asyncExecutor = processEngine.getProcessEngineConfiguration()
            .getAsyncExecutor();
        asyncExecutor.shutdown();
    }

    private void setSecondsToWaitOnJobExecutorShutdown(long secondsToWaitOnShutdown) {
        LOGGER.info(format(Messages.SETTING_SECONDS_TO_WAIT_BEFORE_FLOWABLE_JOB_EXECUTOR_SHUTDOWN, secondsToWaitOnShutdown));
        AsyncExecutor asyncExecutor = processEngine.getProcessEngineConfiguration()
            .getAsyncExecutor();
        ((DefaultAsyncJobExecutor) asyncExecutor).setSecondsToWaitOnShutdown(secondsToWaitOnShutdown);
    }

    public boolean isJobExecutorActive() {
        return processEngine.getProcessEngineConfiguration()
            .getAsyncExecutor()
            .isActive();
    }

    public ProcessEngine getProcessEngine() {
        return processEngine;
    }

    public String findHistoricProcessInstanceIdByProcessDefinitionKey(String processInstanceId, String processDefinitionKey) {
        return findHistoricProcessInstanceIdsAndProcessDefinitionKey(getHistoricSubProcessIds(processInstanceId).stream()
            .collect(Collectors.toSet()), processDefinitionKey);
    }

    private String findHistoricProcessInstanceIdsAndProcessDefinitionKey(Set<String> processInstanceIds, String processDefinitionKey) {
        return processEngine.getHistoryService()
            .createHistoricProcessInstanceQuery()
            .processInstanceIds(processInstanceIds)
            .processDefinitionKey(processDefinitionKey)
            .singleResult()
            .getId();
    }

}

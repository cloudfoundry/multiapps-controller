package com.sap.cloud.lm.sl.cf.core.activiti;

import static java.text.MessageFormat.format;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.persistence.message.Constants;
import com.sap.cloud.lm.sl.cf.web.api.model.State;
import com.sap.cloud.lm.sl.common.util.CommonUtil;

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
            // After the setAuthenticatedUserId() method is invoked, all Activiti service methods
            // executed within the current thread will have access to this userId. Just before
            // leaving the method, the userId is set to null, preventing other services from using
            // it unintentionally.
            processEngine.getIdentityService()
                .setAuthenticatedUserId(null);
        }
    }

    public State getProcessInstanceState(String processInstanceId) {
        if (isProcessInstanceDeleted(processInstanceId)) {
            return State.ABORTED;
        }

        if (isProcessInstanceAtReceiveTask(processInstanceId)) {
            return State.ACTION_REQUIRED;
        }

        if (hasDeadLetterJobs(processInstanceId)) {
            return State.ERROR;
        }

        if (isProcessInRunningState(processInstanceId)) {
            return State.RUNNING;
        }

        if (!isProcessInRunningState(processInstanceId)) {
            return State.FINISHED;
        }

        throw new IllegalStateException("Could not determine process state");
    }

    private boolean isProcessInRunningState(String processId) {
        return hasProcessInstanceNotEnded(processId) || hasHistoricProcessInstanceNotEnded(processId) || hasRunningExecutionJobs(processId);
    }

    private boolean hasRunningExecutionJobs(String processId) {
        return !getRunningJobsForOperation(processId).isEmpty();
    }

    private List<Job> getRunningJobsForOperation(String processInstanceId) {
        return processEngine.getManagementService()
            .createJobQuery()
            .processInstanceId(processInstanceId)
            .list();
    }

    private boolean hasHistoricProcessInstanceNotEnded(String processId) {
        HistoricProcessInstance historicProcessInstance = processEngine.getHistoryService()
            .createHistoricProcessInstanceQuery()
            .processInstanceId(processId)
            .excludeSubprocesses(true)
            .singleResult();
        return historicProcessInstance.getEndActivityId() == null;
    }

    private boolean hasProcessInstanceNotEnded(String processId) {
        ProcessInstance processInstance = processEngine.getRuntimeService()
            .createProcessInstanceQuery()
            .processInstanceId(processId)
            .singleResult();
        return processInstance != null && !processInstance.isEnded();
    }

    private boolean isProcessInstanceDeleted(String processId) {
        return processEngine.getRuntimeService()
            .createProcessInstanceQuery()
            .processInstanceId(processId)
            .singleResult() == null && hasDeleteReason(processId);
    }

    private boolean hasDeleteReason(String processId) {
        HistoricProcessInstance historicProcessInstance = processEngine.getHistoryService()
            .createHistoricProcessInstanceQuery()
            .processInstanceId(processId)
            .singleResult();
        return historicProcessInstance != null ? !CommonUtil.isNullOrEmpty(historicProcessInstance.getDeleteReason()) : false;
    }

    private boolean hasDeadLetterJobs(String processId) {
        return !getDeadLetterJobs(processId).isEmpty() || !getDeadLetterJobsInSubProcesses(processId).isEmpty();
    }

    private List<Job> getDeadLetterJobsInSubProcesses(String processId) {
        List<String> subProcessIds = getHistoricSubProcessIds(processId);
        List<Job> deadLetterJobs = subProcessIds.stream()
            .map(subProcessId -> getDeadLetterJobs(subProcessId))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        return deadLetterJobs;
    }

    private List<Job> getDeadLetterJobs(String processId) {
        return processEngine.getManagementService()
            .createDeadLetterJobQuery()
            .processInstanceId(processId)
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
        return getHistoricActivitiInstance(processId, "endEvent") == null;
    }

    private HistoricActivityInstance getHistoricActivitiInstance(String processId, String activityType) {
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
        List<Execution> allProcessExecutions = getProcessExecutions(processInstanceId);

        return allProcessExecutions.stream()
            .filter(execution -> !findCurrentActivitiesAtReceiveTask(execution).isEmpty())
            .collect(Collectors.toList());

    }

    public List<Execution> getProcessExecutions(String processInstanceId) {
        List<Execution> allProcessExecutions = processEngine.getRuntimeService()
            .createExecutionQuery()
            .rootProcessInstanceId(processInstanceId)
            .list();

        return allProcessExecutions.stream()
            .filter(e -> e.getActivityId() != null)
            .collect(Collectors.toList());
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
        ProcessInstance processInstance = processEngine.getRuntimeService()
            .createProcessInstanceQuery()
            .processInstanceId(processInstanceId)
            .singleResult();
        return processInstance != null && processInstance.isSuspended();
    }

}

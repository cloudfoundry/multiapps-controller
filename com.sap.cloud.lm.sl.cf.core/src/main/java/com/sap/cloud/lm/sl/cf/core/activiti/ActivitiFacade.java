package com.sap.cloud.lm.sl.cf.core.activiti;

import static java.text.MessageFormat.format;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.activiti.engine.ActivitiOptimisticLockingException;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.persistence.message.Constants;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.State;

@Component
public class ActivitiFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivitiFacade.class);
    private static final int DEFAULT_ABORT_TIMEOUT_MS = 30000;

    private final ProcessEngine processEngine;

    @Inject
    public ActivitiFacade(ProcessEngine processEngine) {
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

    public State getOngoingOperationState(Operation ongoingOperation) {
        Job executionJob = getJob(ongoingOperation.getProcessId());
        if (isProcessInstanceSuspended(ongoingOperation.getProcessId())) {
            return State.ACTION_REQUIRED;
        }
        if (executionJob != null) {
            return executionJob.getExceptionMessage() != null ? State.ERROR : State.RUNNING;
        }
        return State.ACTION_REQUIRED;
    }

    public List<HistoricActivityInstance> getHistoricActivities(String activityType, String processInstanceId) {
        return processEngine.getHistoryService()
            .createHistoricActivityInstanceQuery()
            .activityType(activityType)
            .processInstanceId(processInstanceId)
            .orderByHistoricActivityInstanceStartTime()
            .asc()
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
            .processInstanceId(processInstanceId)
            .list();
    }

    public void executeJob(String userId, String processInstanceId) {
        Job job = getJob(processInstanceId);
        if (job == null) {
            return;
        }
        try {
            processEngine.getIdentityService()
                .setAuthenticatedUserId(userId);
            processEngine.getManagementService()
                .executeJob(job.getId());
        } finally {
            processEngine.getIdentityService()
                .setAuthenticatedUserId(null);
        }
    }

    private Job getJob(String processInstanceId) {
        return processEngine.getManagementService()
            .createJobQuery()
            .processInstanceId(processInstanceId)
            .singleResult();
    }

    public void signal(String userId, String executionId) {
        try {
            processEngine.getIdentityService()
                .setAuthenticatedUserId(userId);
            processEngine.getRuntimeService()
                .signal(executionId);
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
                    LOGGER.debug(format(Messages.SET_SUCCESSFULLY, Constants.PROCESS_ABORTED));

                    processEngine.getRuntimeService()
                        .deleteProcessInstance(processInstanceId, deleteReason);
                    break;
                } catch (ActivitiOptimisticLockingException e) {
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

    public boolean isProcessInstanceSuspended(String processInstanceId) {
        ProcessInstance processInstance = processEngine.getRuntimeService()
            .createProcessInstanceQuery()
            .processInstanceId(processInstanceId)
            .singleResult();
        return processInstance != null && processInstance.isSuspended();
    }

    public void activateProcessInstance(String processInstanceId) {
        processEngine.getRuntimeService()
            .activateProcessInstanceById(processInstanceId);
    }

    public Set<String> getAllVariableNames(String processInstanceId) {
        return processEngine.getHistoryService()
            .createHistoricVariableInstanceQuery()
            .processInstanceId(processInstanceId)
            .list()
            .stream()
            .map(HistoricVariableInstance::getVariableName)
            .collect(Collectors.toSet());
    }

    public void suspendProcessInstance(String processInstanceId) {
        processEngine.getRuntimeService()
            .suspendProcessInstanceById(processInstanceId);
    }

    protected boolean isPastDeadline(long deadline) {
        return System.currentTimeMillis() >= deadline;
    }

}

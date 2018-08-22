package com.sap.cloud.lm.sl.cf.core.activiti;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.activiti.engine.ActivitiOptimisticLockingException;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.persistence.message.Constants;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.State;

public class ActivitiFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivitiFacade.class);
    private static final int DEFAULT_ABORT_TIMEOUT_MS = 30000;

    private ProcessEngine engine;

    private static final ActivitiFacade INSTANCE = new ActivitiFacade();

    public static ActivitiFacade getInstance() {
        return INSTANCE;
    }

    protected ActivitiFacade() {
    }

    public void init(ProcessEngine engine) {
        this.engine = engine;
    }

    public ProcessInstance startProcess(String userId, String processDefinitionKey, Map<String, Object> variables) {
        try {
            engine.getIdentityService()
                .setAuthenticatedUserId(userId);
            return engine.getRuntimeService()
                .startProcessInstanceByKey(processDefinitionKey, variables);
        } finally {
            // After the setAuthenticatedUserId() method is invoked, all Activiti service methods
            // executed within the current thread will have access to this userId. Just before
            // leaving the method, the userId is set to null, preventing other services from using
            // it unintentionally.
            engine.getIdentityService()
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
        return engine.getHistoryService()
            .createHistoricActivityInstanceQuery()
            .activityType(activityType)
            .processInstanceId(processInstanceId)
            .orderByHistoricActivityInstanceStartTime()
            .asc()
            .list();
    }

    public List<String> getHistoricSubProcessIds(String correlationId) {
        List<String> subProcessIds = new ArrayList<>();
        List<HistoricVariableInstance> variablesWithCorrelationId = retrieveVariablesByCorrelationId(correlationId);
        for (HistoricVariableInstance historicVariableInstance : variablesWithCorrelationId) {
            if (!historicVariableInstance.getProcessInstanceId()
                .equals(correlationId)) {
                subProcessIds.add(historicVariableInstance.getProcessInstanceId());
            }
        }
        return subProcessIds;
    }

    private List<HistoricVariableInstance> retrieveVariablesByCorrelationId(String correlationId) {
        return engine.getHistoryService()
            .createHistoricVariableInstanceQuery()
            .variableValueEquals(Constants.CORRELATION_ID, correlationId)
            .orderByProcessInstanceId()
            .asc()
            .list();
    }

    public HistoricVariableInstance getHistoricVariableInstance(String processInstanceId, String variableName) {
        return engine.getHistoryService()
            .createHistoricVariableInstanceQuery()
            .processInstanceId(processInstanceId)
            .variableName(variableName)
            .singleResult();
    }

    public List<String> getActiveHistoricSubProcessIds(String superProcessId) {
        List<String> subProcessIds = getHistoricSubProcessIds(superProcessId);
        List<String> activeSubProcessIds = new ArrayList<>();
        for (String subProcessId : subProcessIds) {
            HistoricActivityInstance subProcessEndActivity = getHistoricActivitiInstance(subProcessId, "endEvent");
            if (subProcessEndActivity == null) {
                activeSubProcessIds.add(subProcessId);
            }
        }
        return activeSubProcessIds;
    }

    private HistoricActivityInstance getHistoricActivitiInstance(String processId, String activityType) {
        return engine.getHistoryService()
            .createHistoricActivityInstanceQuery()
            .activityType(activityType)
            .processInstanceId(processId)
            .singleResult();
    }

    public String getActivityType(String processInstanceId, String executionId, String activityId) {
        List<HistoricActivityInstance> historicInstancesList = engine.getHistoryService()
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
        List<Execution> executionQueryByProcessId = getExecutionsByProcessId(processInstanceId);
        for (Execution execution : executionQueryByProcessId) {
            if (execution.getActivityId() != null) {
                return execution;
            }
        }
        return null;
    }

    private List<Execution> getExecutionsByProcessId(String processInstanceId) {
        return engine.getRuntimeService()
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
            engine.getIdentityService()
                .setAuthenticatedUserId(userId);
            engine.getManagementService()
                .executeJob(job.getId());
        } finally {
            engine.getIdentityService()
                .setAuthenticatedUserId(null);
        }
    }

    private Job getJob(String processInstanceId) {
        return engine.getManagementService()
            .createJobQuery()
            .processInstanceId(processInstanceId)
            .singleResult();
    }

    public void signal(String userId, String executionId) {
        try {
            engine.getIdentityService()
                .setAuthenticatedUserId(userId);
            engine.getRuntimeService()
                .signal(executionId);
        } finally {
            engine.getIdentityService()
                .setAuthenticatedUserId(null);
        }
    }

    public void deleteProcessInstance(String userId, String processInstanceId, String deleteReason) {
        try {
            engine.getIdentityService()
                .setAuthenticatedUserId(userId);

            long deadline = System.currentTimeMillis() + DEFAULT_ABORT_TIMEOUT_MS;
            while (true) {
                try {
                    LOGGER.debug(format(Messages.SETTING_VARIABLE, Constants.PROCESS_ABORTED, Boolean.TRUE));
                    // TODO: Use execution ID instead of process instance ID, as they can be
                    // different if the process has parallel executions.
                    engine.getRuntimeService()
                        .setVariable(processInstanceId, Constants.PROCESS_ABORTED, Boolean.TRUE);
                    LOGGER.debug(format(Messages.SET_SUCCESSFULLY, Constants.PROCESS_ABORTED));

                    engine.getRuntimeService()
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
            engine.getIdentityService()
                .setAuthenticatedUserId(null);
        }
    }

    public boolean isProcessInstanceSuspended(String processInstanceId) {
        ProcessInstance processInstance = engine.getRuntimeService()
            .createProcessInstanceQuery()
            .processInstanceId(processInstanceId)
            .singleResult();
        return processInstance != null && processInstance.isSuspended();
    }

    public void activateProcessInstance(String processInstanceId) {
        engine.getRuntimeService()
            .activateProcessInstanceById(processInstanceId);
    }

    public Set<String> getAllVariableNames(String processInstanceId) {
        return engine.getHistoryService()
            .createHistoricVariableInstanceQuery()
            .processInstanceId(processInstanceId)
            .list()
            .stream()
            .map(HistoricVariableInstance::getVariableName)
            .collect(Collectors.toSet());
    }

    public void suspendProcessInstance(String processInstanceId) {
        engine.getRuntimeService()
            .suspendProcessInstanceById(processInstanceId);
    }

    protected boolean isPastDeadline(long deadline) {
        return System.currentTimeMillis() >= deadline;
    }

}

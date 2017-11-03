package com.sap.cloud.lm.sl.cf.api.activiti;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.activiti.engine.ActivitiOptimisticLockingException;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.history.HistoricVariableInstanceQuery;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ExecutionQuery;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.JobQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.cf.web.api.model.State;

public class ActivitiFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivitiFacade.class);
    private static final int DEFAULT_ABORT_TIMEOUT_MS = 30000;
    private static final long GET_EXECUTION_RETRY_INTERVAL_MS = 100;

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
            engine.getIdentityService().setAuthenticatedUserId(userId);
            // Get the last deployed version of the process and start a process instance
            ProcessDefinitionQuery query = engine.getRepositoryService().createProcessDefinitionQuery().processDefinitionKey(
                processDefinitionKey).latestVersion();
            String processDefinitionId = query.singleResult().getId();
            ProcessInstance processInstance = engine.getRuntimeService().startProcessInstanceById(processDefinitionId, variables);
            return processInstance;
        } finally {
            // After the setAuthenticatedUserId() method is invoked, all Activiti service methods
            // executed within the current thread will have access to this userId. Just before
            // leaving the method, the userId is set to null, preventing other services from using
            // it unintentionally.
            engine.getIdentityService().setAuthenticatedUserId(null);
        }
    }

    public HistoricProcessInstance getHistoricProcessInstanceBySpaceId(String processDefinitionKey, String spaceId,
        String processInstanceId) {
        HistoricProcessInstanceQuery query = engine.getHistoryService().createHistoricProcessInstanceQuery().processDefinitionKey(
            processDefinitionKey).variableValueEquals(Constants.VARIABLE_NAME_SPACE_ID, spaceId).excludeSubprocesses(
                true).processInstanceId(processInstanceId);

        return query.singleResult();
    }

    public HistoricProcessInstance getHistoricProcessInstanceByProcessInstanceId(String processInstanceId) {
        return engine.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();

    }

    public State getOngoingOperationState(Operation ongoingOperation) {
        Job executionJob = engine.getManagementService().createJobQuery().processInstanceId(ongoingOperation.getProcessId()).singleResult();
        if (executionJob != null) {
            return executionJob.getExceptionMessage() != null ? State.ERROR : State.RUNNING;
        }
        return State.ACTION_REQUIRED;
    }

    public List<HistoricActivityInstance> getHistoricActivities(String activityType, String processInstanceId) {
        return engine.getHistoryService().createHistoricActivityInstanceQuery().activityType(activityType).processInstanceId(
            processInstanceId).orderByHistoricActivityInstanceStartTime().asc().list();
    }

    public List<String> getHistoricSubProcessIds(String superProcessId) {
        List<String> subProcessIds = new ArrayList<>();
        List<HistoricVariableInstance> variablesWithSuperProcessId = retrieveVariablesByCorrelationId(superProcessId);
        for (HistoricVariableInstance historicVariableInstance : variablesWithSuperProcessId) {
            if (!historicVariableInstance.getProcessInstanceId().equals(superProcessId)) {
                subProcessIds.add(historicVariableInstance.getProcessInstanceId());
            }
        }

        return subProcessIds;
    }

    List<HistoricVariableInstance> retrieveVariablesByCorrelationId(String superProcessId) {
        return engine.getHistoryService().createHistoricVariableInstanceQuery().variableValueEquals(Constants.CORRELATION_ID,
            superProcessId).orderByProcessInstanceId().asc().list();
    }

    public List<Execution> getProcessExecutions(String processInstanceId) {
        return getExecutionQuery(processInstanceId).list();
    }

    private ExecutionQuery getExecutionQuery(String processInstanceId) {
        return engine.getRuntimeService().createExecutionQuery().processInstanceId(processInstanceId);
    }

    public HistoricVariableInstance getHistoricVariableInstance(String processInstanceId, String variableName) {
        HistoricVariableInstanceQuery query = engine.getHistoryService().createHistoricVariableInstanceQuery().processInstanceId(
            processInstanceId).variableName(variableName);
        return query.singleResult();
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

    HistoricActivityInstance getHistoricActivitiInstance(String processId, String activityType) {
        return engine.getHistoryService().createHistoricActivityInstanceQuery().activityType(activityType).processInstanceId(
            processId).singleResult();
    }

    public String getActivityType(String processInstanceId, String activityId) {
        HistoricActivityInstance historicInstance = engine.getHistoryService().createHistoricActivityInstanceQuery().processInstanceId(
            processInstanceId).activityId(activityId).singleResult();
        return historicInstance != null ? historicInstance.getActivityType() : null;
    }

    public Execution getProcessExecution(String processInstanceId) {
        return getExecutionQuery(processInstanceId).singleResult();
    }

    public void executeJob(String userId, String processInstanceId) {
        Job job = getJob(processInstanceId);
        if (job != null) {
            try {
                engine.getIdentityService().setAuthenticatedUserId(userId);
                engine.getManagementService().executeJob(job.getId());
            } finally {
                engine.getIdentityService().setAuthenticatedUserId(null);
            }
        }
    }

    public Job getJob(String processInstanceId) {
        JobQuery query = engine.getManagementService().createJobQuery().processInstanceId(processInstanceId);
        return query.singleResult();
    }

    public void signal(String userId, String executionId) {
        try {
            engine.getIdentityService().setAuthenticatedUserId(userId);
            engine.getRuntimeService().signal(executionId);
        } finally {
            engine.getIdentityService().setAuthenticatedUserId(null);
        }
    }

    public void signal(String userId, String processId, String activityId, Map<String, Object> variables, long timeoutInMillis) {
        String executionId = getExecutionId(processId, activityId, timeoutInMillis);
        LOGGER.info(format("Found execution with id:{0} for process with id:{1} and activity id:{2}", executionId, processId, activityId));
        try {
            engine.getIdentityService().setAuthenticatedUserId(userId);
            engine.getRuntimeService().signal(executionId, variables);
        } catch (Exception e) { // NOSONAR
            LOGGER.error(format("Failed to signal execution with id:{0} for process with id:{1} and activity id:{2}", executionId,
                processId, activityId), e);
            throw e;
        } finally {
            engine.getIdentityService().setAuthenticatedUserId(null);
        }

    }

    private String getExecutionId(String processId, String activityId, long timeoutInMillis) {
        long deadline = System.currentTimeMillis() + timeoutInMillis;
        while (true) {
            Execution execution = engine.getRuntimeService().createExecutionQuery().processInstanceId(processId).activityId(
                activityId).singleResult();
            if (execution != null && execution.getParentId() != null) {
                return execution.getId();
            }
            if (isPastDeadline(deadline)) {
                IllegalStateException timeoutException = new IllegalStateException(
                    format(Messages.PROCESS_STEP_NOT_REACHED_BEFORE_TIMEOUT, activityId, processId));
                LOGGER.error(timeoutException.toString(), timeoutException);
                throw timeoutException;
            }
            try {
                Thread.sleep(GET_EXECUTION_RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public void deleteProcessInstance(String userId, String processInstanceId, String deleteReason) {
        try {
            engine.getIdentityService().setAuthenticatedUserId(userId);

            long deadline = System.currentTimeMillis() + DEFAULT_ABORT_TIMEOUT_MS;
            while (true) {
                try {
                    LOGGER.debug(format(Messages.SETTING_VARIABLE, Constants.PROCESS_ABORTED, Boolean.TRUE));
                    // TODO: Use execution ID instead of process instance ID, as they can be
                    // different if the process has parallel executions.
                    engine.getRuntimeService().setVariable(processInstanceId, Constants.PROCESS_ABORTED, Boolean.TRUE);
                    LOGGER.debug(format(Messages.SET_SUCCESSFULLY, Constants.PROCESS_ABORTED));

                    engine.getRuntimeService().deleteProcessInstance(processInstanceId, deleteReason);
                    break;
                } catch (ActivitiOptimisticLockingException e) {
                    if (isPastDeadline(deadline)) {
                        throw new IllegalStateException(Messages.ABORT_OPERATION_TIMED_OUT, e);
                    }
                    LOGGER.warn(format(Messages.RETRYING_PROCESS_ABORT, processInstanceId));
                }
            }
        } finally {
            engine.getIdentityService().setAuthenticatedUserId(null);
        }
    }

    protected boolean isPastDeadline(long deadline) {
        return System.currentTimeMillis() >= deadline;
    }

    public void setRuntimeVariables(String processInstanceId, Map<String, Object> variables) {
        engine.getRuntimeService().setVariables(processInstanceId, variables);
    }

}

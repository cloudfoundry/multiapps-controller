package com.sap.cloud.lm.sl.cf.process.flowable;

import static java.text.MessageFormat.format;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.persistence.Constants;
import com.sap.cloud.lm.sl.cf.web.api.model.State;

@Named
public class FlowableFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableFacade.class);

    private static final int DEFAULT_JOB_RETRIES = 0;
    private static final int DEFAULT_ABORT_TIMEOUT_MS = 30000;

    private final ProcessEngine processEngine;

    @Inject
    public FlowableFacade(ProcessEngine processEngine) {
        this.processEngine = processEngine;
    }

    public ProcessInstance startProcess(String processDefinitionKey, Map<String, Object> variables) {
        return processEngine.getRuntimeService()
                            .startProcessInstanceByKey(processDefinitionKey, variables);
    }

    public String getProcessInstanceId(String executionId) {
        return getVariable(executionId, Constants.CORRELATION_ID);
    }

    public String getCurrentTaskId(String executionId) {
        return getVariable(executionId, Constants.TASK_ID);
    }

    private String getVariable(String executionId, String variableName) {
        VariableInstance variableInstance = processEngine.getRuntimeService()
                                                         .getVariableInstance(executionId, variableName);

        if (variableInstance == null) {
            return getVariableFromHistoryService(executionId, variableName);
        }

        return variableInstance.getTextValue();
    }

    private String getVariableFromHistoryService(String executionId, String variableName) {
        HistoricVariableInstance historicVariableInstance = processEngine.getHistoryService()
                                                                         .createHistoricVariableInstanceQuery()
                                                                         .executionId(executionId)
                                                                         .variableName(variableName)
                                                                         .singleResult();

        if (historicVariableInstance == null) {
            return null;
        }

        return (String) historicVariableInstance.getValue();
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
                                   .flatMap(List::stream)
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
                                                      .collect(Collectors.toCollection(LinkedList::new));
    }

    private boolean isActive(String processId) {
        return processEngine.getHistoryService()
                            .createHistoricActivityInstanceQuery()
                            .activityType("endEvent")
                            .processInstanceId(processId)
                            .singleResult() == null;
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
                                                                       .getActivityType()
            : null;
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

    public void executeJob(String processInstanceId) {
        List<Job> deadLetterJobs = getDeadLetterJobs(processInstanceId);
        if (deadLetterJobs.isEmpty()) {
            LOGGER.info(MessageFormat.format("No dead letter jobs found for process with id {0}", processInstanceId));
            return;
        }
        moveDeadLetterJobsToExecutableJobs(deadLetterJobs);
    }

    private void moveDeadLetterJobsToExecutableJobs(List<Job> deadLetterJobs) {
        for (Job deadLetterJob : deadLetterJobs) {
            moveDeadLetterJobToExecutableJob(deadLetterJob);
        }
    }

    private void moveDeadLetterJobToExecutableJob(Job deadLetterJob) {
        processEngine.getManagementService()
                     .moveDeadLetterJobToExecutableJob(deadLetterJob.getId(), DEFAULT_JOB_RETRIES);
    }

    public void trigger(String executionId, Map<String, Object> variables) {
        processEngine.getRuntimeService()
                     .trigger(executionId, variables);
    }

    public void deleteProcessInstance(String processInstanceId, String deleteReason) {
        long deadline = System.currentTimeMillis() + DEFAULT_ABORT_TIMEOUT_MS;
        while (true) {
            try {
                LOGGER.debug(format(Messages.SETTING_VARIABLE, Constants.PROCESS_ABORTED, Boolean.TRUE));

                // TODO: Use execution ID instead of process instance ID, as
                // they can be
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

    public void shutdownJobExecutor() {
        LOGGER.info(Messages.SHUTTING_DOWN_FLOWABLE_JOB_EXECUTOR);
        AsyncExecutor asyncExecutor = processEngine.getProcessEngineConfiguration()
                                                   .getAsyncExecutor();
        asyncExecutor.shutdown();
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
        return findHistoricProcessInstanceIdsAndProcessDefinitionKey(new HashSet<>(getHistoricSubProcessIds(processInstanceId)),
                                                                     processDefinitionKey);
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

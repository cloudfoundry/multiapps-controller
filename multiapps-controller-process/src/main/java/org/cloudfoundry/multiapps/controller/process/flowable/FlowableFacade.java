package org.cloudfoundry.multiapps.controller.process.flowable;

import static java.text.MessageFormat.format;

import java.text.MessageFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
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

@Named
public class FlowableFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableFacade.class);

    private static final int DEFAULT_JOB_RETRIES = 0;
    private static final int DEFAULT_ABORT_TIMEOUT_MS = 30 * 1000;
    private static final int DEFAULT_ABORT_WAIT_TIMEOUT_MS = 60 * 1000;

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

    public ProcessInstance getProcessInstance(String processId) {
        return processEngine.getRuntimeService()
                            .createProcessInstanceQuery()
                            .processInstanceId(processId)
                            .singleResult();
    }

    public boolean hasDeadLetterJobs(String processId) {
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
        List<HistoricVariableInstance> historicVariableInstances = retrieveVariablesByCorrelationId(correlationId);
        return historicVariableInstances.stream()
                                        .map(HistoricVariableInstance::getProcessInstanceId)
                                        .filter(id -> !id.equals(correlationId))
                                        .collect(Collectors.toList());
    }

    public HistoricProcessInstance getHistoricProcessById(String processInstanceId) {
        return processEngine.getHistoryService()
                            .createHistoricProcessInstanceQuery()
                            .processInstanceId(processInstanceId)
                            .singleResult();
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
        HistoricProcessInstance processInstance = processEngine.getHistoryService()
                                                               .createHistoricProcessInstanceQuery()
                                                               .processInstanceId(processId)
                                                               .singleResult();
        return processInstance.getEndTime() == null;
    }

    public void executeJob(String processInstanceId) {
        List<String> deadLetterJobsIds = getDeadLetterJobsDistinctIds(processInstanceId);
        if (deadLetterJobsIds.isEmpty()) {
            LOGGER.info(MessageFormat.format("No dead letter jobs found for process with id {0}", processInstanceId));
            return;
        }
        moveDeadLetterJobsToExecutableJobs(deadLetterJobsIds);
    }

    private List<String> getDeadLetterJobsDistinctIds(String processInstanceId) {
        return getDeadLetterJobs(processInstanceId).stream()
                                                   .map(Job::getId)
                                                   .distinct()
                                                   .collect(Collectors.toList());
    }

    private void moveDeadLetterJobsToExecutableJobs(List<String> deadLetterJobIds) {
        deadLetterJobIds.forEach(this::moveDeadLetterJobToExecutableJob);
    }

    private void moveDeadLetterJobToExecutableJob(String deadLetterJobId) {
        try {
            processEngine.getManagementService()
                         .moveDeadLetterJobToExecutableJob(deadLetterJobId, DEFAULT_JOB_RETRIES);
        } catch (FlowableObjectNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public void trigger(String executionId, Map<String, Object> variables) {
        processEngine.getRuntimeService()
                     .trigger(executionId, variables);
    }

    public void deleteProcessInstance(String processInstanceId, String deleteReason) {
        long overallAbortDeadline = System.currentTimeMillis() + DEFAULT_ABORT_WAIT_TIMEOUT_MS + DEFAULT_ABORT_TIMEOUT_MS;
        while (true) {
            try {
                processEngine.getRuntimeService()
                             .deleteProcessInstance(processInstanceId, deleteReason);
                break;
            } catch (FlowableOptimisticLockingException e) {
                if (isPastDeadline(overallAbortDeadline)) {
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

    public void suspendProcessInstance(String processInstanceId) {
        processEngine.getRuntimeService()
                     .suspendProcessInstanceById(processInstanceId);
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

    public List<ProcessInstance> findAllRunningProcessInstanceStartedBefore(Date startedBefore) {
        return processEngine.getRuntimeService()
                            .createProcessInstanceQuery()
                            .excludeSubprocesses(true)
                            .startedBefore(startedBefore)
                            .list();
    }

    public Execution getParentExecution(String parentId) {
        return processEngine.getRuntimeService()
                            .createExecutionQuery()
                            .executionId(parentId)
                            .singleResult();
    }

}

package com.sap.cloud.lm.sl.cf.process.flowable;

import static com.sap.cloud.lm.sl.cf.process.flowable.Messages.ABORT_OPERATION_FAILED;
import static com.sap.cloud.lm.sl.cf.process.flowable.Messages.ABORT_OPERATION_FOR_PROCESS_0_FAILED;
import static com.sap.cloud.lm.sl.cf.process.flowable.Messages.CURRENT_EXECUTION_WITHOUT_CHILDREN_0;
import static com.sap.cloud.lm.sl.cf.process.flowable.Messages.DEAD_LETTER_JOBS_FOR_PROCESS_0_1;
import static com.sap.cloud.lm.sl.cf.process.flowable.Messages.EXECUTION_0_DOES_NOT_HAVE_DEAD_LETTER_JOBS;
import static com.sap.cloud.lm.sl.cf.process.flowable.Messages.NEW_ACTIVE_EXECUTIONS_WITHOUT_CHILDREN_0_FOR_PROCESS_1;
import static com.sap.cloud.lm.sl.cf.process.flowable.Messages.NO_DEAD_LETTER_JOBS_FOUND_FOR_PROCESS_WITH_ID_0;
import static com.sap.cloud.lm.sl.cf.process.flowable.Messages.PARENT_EXECUTION_WILL_NOT_BE_WAITED_TO_FINISH;
import static com.sap.cloud.lm.sl.cf.process.flowable.Messages.PROCESS_0_HAS_ALREADY_BEEN_SUSPENDED;
import static com.sap.cloud.lm.sl.cf.process.flowable.Messages.PROCESS_0_HAS_BEEN_DELETED_SUCCESSFULLY;
import static com.sap.cloud.lm.sl.cf.process.flowable.Messages.PROCESS_0_HAS_BEEN_SUSPENDED_SUCCESSFULLY;
import static com.sap.cloud.lm.sl.cf.process.flowable.Messages.PROCESS_INSTANCE_0_IS_AT_RECEIVE_TASK;
import static com.sap.cloud.lm.sl.cf.process.flowable.Messages.PROCESS_WITH_ID_0_NOT_FOUND_WHILE_WAITING_FOR_EXECUTION_1_TO_FINISH;
import static com.sap.cloud.lm.sl.cf.process.flowable.Messages.TIMEOUT_OF_0_FOR_PROCESS_1_HAS_BEEN_REACHED;
import static com.sap.cloud.lm.sl.cf.process.flowable.Messages.TIMER_EXECUTION_WILL_NOT_BE_WAITED_TO_FINISH;
import static java.text.MessageFormat.format;

import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.flowable.variable.api.persistence.entity.VariableInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.persistence.Constants;
import com.sap.cloud.lm.sl.common.NotFoundException;

@Named
public class FlowableFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableFacade.class);

    private static final int DEFAULT_JOB_RETRIES = 0;
    private static final int DEFAULT_ABORT_WAIT_TIMEOUT_MS = 3 * 60 * 1000;

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
                                   .map(this::getDeadLetterJobsByProcessInstanceId)
                                   .flatMap(List::stream)
                                   .collect(Collectors.toList());
    }

    private List<Job> getDeadLetterJobsByProcessInstanceId(Execution execution) {
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
        return processEngine.getHistoryService()
                            .createHistoricActivityInstanceQuery()
                            .activityType("endEvent")
                            .processInstanceId(processId)
                            .singleResult() == null;
    }

    private Optional<Execution> getRootExecution(String processInstanceId) {
        return Optional.ofNullable(processEngine.getRuntimeService()
                                                .createExecutionQuery()
                                                .processInstanceId(processInstanceId)
                                                .executionId(processInstanceId)
                                                .rootProcessInstanceId(processInstanceId)
                                                .singleResult());
    }

    public void executeJob(String processInstanceId) {
        List<Job> deadLetterJobs = getDeadLetterJobs(processInstanceId);
        if (deadLetterJobs.isEmpty()) {
            LOGGER.debug(MessageFormat.format(NO_DEAD_LETTER_JOBS_FOUND_FOR_PROCESS_WITH_ID_0, processInstanceId));
            return;
        }
        LOGGER.debug(MessageFormat.format(DEAD_LETTER_JOBS_FOR_PROCESS_0_1, processInstanceId, deadLetterJobs));
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
        if (isProcessInstanceAtReceiveTask(processInstanceId)) {
            LOGGER.debug(format(PROCESS_INSTANCE_0_IS_AT_RECEIVE_TASK, processInstanceId));
            abortProcessInstance(processInstanceId, deleteReason);
            return;
        }
        try {
            abortProcessSafely(processInstanceId, deleteReason);
        } catch (NotFoundException e) {
            LOGGER.error(MessageFormat.format(ABORT_OPERATION_FAILED, e.getMessage()), e);
        }
    }

    private void abortProcessSafely(String processInstanceId, String deleteReason) {
        // Get all active leaf executions before aborting all running processes
        List<Execution> allActiveExecutionsWithoutChildren = getLeafExecutionsByFilter(processInstanceId, ExecutionEntityImpl::isActive);
        setAbortProcessVariableInContext(processInstanceId);
        waitAllActiveExecutionsToFinish(processInstanceId, allActiveExecutionsWithoutChildren);
        suspendProcessIfNotSuspended(processInstanceId);
        abortProcessInstance(processInstanceId, deleteReason);
    }

    private void setAbortProcessVariableInContext(String processInstanceId) {
        LOGGER.debug(format(Messages.SETTING_VARIABLE, Constants.PROCESS_ABORTED, Boolean.TRUE));
        processEngine.getRuntimeService()
                     .setVariable(processInstanceId, Constants.PROCESS_ABORTED, Boolean.TRUE);
    }

    private void suspendProcessIfNotSuspended(String processInstanceId) {
        if (isProcessSuspended(processInstanceId)) {
            LOGGER.debug(format(PROCESS_0_HAS_ALREADY_BEEN_SUSPENDED, processInstanceId));
            return;
        }
        suspendProcessInstance(processInstanceId);
    }

    private boolean isProcessSuspended(String processInstanceId) {
        Optional<Execution> rootProcessExecution = getRootExecution(processInstanceId);
        return rootProcessExecution.isPresent() && rootProcessExecution.get()
                                                                       .isSuspended();
    }

    private void waitAllActiveExecutionsToFinish(String processInstanceId, List<Execution> allActiveExecutionsWithoutChildren) {
        Queue<Execution> activeLeafExecutions = new ArrayDeque<>(allActiveExecutionsWithoutChildren);
        long allSubprocessesFinishedDeadline = System.currentTimeMillis() + DEFAULT_ABORT_WAIT_TIMEOUT_MS;
        while (!activeLeafExecutions.isEmpty()) {
            if (isPastDeadline(allSubprocessesFinishedDeadline)) {
                LOGGER.debug(format(TIMEOUT_OF_0_FOR_PROCESS_1_HAS_BEEN_REACHED, DEFAULT_ABORT_WAIT_TIMEOUT_MS, processInstanceId));
                return;
            }
            List<Execution> newActiveLeafExecutions = getNewActiveLeafExecutions(processInstanceId, activeLeafExecutions);
            LOGGER.debug(format(NEW_ACTIVE_EXECUTIONS_WITHOUT_CHILDREN_0_FOR_PROCESS_1, newActiveLeafExecutions, processInstanceId));
            Execution execution = activeLeafExecutions.poll();
            activeLeafExecutions.addAll(newActiveLeafExecutions);
            if (shouldSkipExecution(processInstanceId, execution)) {
                continue;
            }
            if (!doesExecutionHaveDeadLetterJobs(execution)) {
                LOGGER.debug(MessageFormat.format(EXECUTION_0_DOES_NOT_HAVE_DEAD_LETTER_JOBS, execution.getId()));
                activeLeafExecutions.add(execution);
            }
            throwExceptionIfProcessNotFound(processInstanceId, activeLeafExecutions);
        }
    }

    private List<Execution> getNewActiveLeafExecutions(String processInstanceId, Queue<Execution> executions) {
        List<Execution> activeExecutionsWithoutChildren = getLeafExecutionsByFilter(processInstanceId, ExecutionEntityImpl::isActive);
        List<String> executionIds = toExecutionIds(executions);
        return activeExecutionsWithoutChildren.stream()
                                              .filter(execution -> !executionIds.contains(execution.getId()))
                                              .collect(Collectors.toList());
    }

    private List<String> toExecutionIds(Collection<Execution> executions) {
        return executions.stream()
                         .map(Execution::getId)
                         .collect(Collectors.toList());
    }

    private boolean shouldSkipExecution(String processInstanceId, Execution execution) {
        if (isParenExecution(processInstanceId, execution)) {
            LOGGER.debug(MessageFormat.format(PARENT_EXECUTION_WILL_NOT_BE_WAITED_TO_FINISH, execution.getId()));
            return true;
        }
        if (isTimerExecution(execution)) {
            LOGGER.debug(MessageFormat.format(TIMER_EXECUTION_WILL_NOT_BE_WAITED_TO_FINISH, execution.getId()));
            return true;
        }
        return false;
    }

    private boolean isParenExecution(String processInstanceId, Execution execution) {
        List<String> executionsWithoutChildrenIds = toExecutionIds(getAllExecutionsWithoutChildren(processInstanceId));
        LOGGER.debug(MessageFormat.format(CURRENT_EXECUTION_WITHOUT_CHILDREN_0, executionsWithoutChildrenIds));
        return !executionsWithoutChildrenIds.contains(execution.getId());
    }

    private boolean isTimerExecution(Execution execution) {
        return getTimerJob(execution.getId()).isPresent();
    }

    private Optional<Job> getTimerJob(String executionId) {
        return Optional.ofNullable(processEngine.getManagementService()
                                                .createTimerJobQuery()
                                                .executionId(executionId)
                                                .singleResult());
    }

    public void setAbortVariable(String processInstanceId) {
        processEngine.getRuntimeService()
                     .setVariable(processInstanceId, Constants.PROCESS_ABORTED, Boolean.TRUE);
    }

    protected boolean isPastDeadline(long deadline) {
        return System.currentTimeMillis() >= deadline;
    }

    private void throwExceptionIfProcessNotFound(String processInstanceId, Queue<Execution> activeLeafExecutions) {
        if (getProcessInstance(processInstanceId) == null) {
            throw new NotFoundException(PROCESS_WITH_ID_0_NOT_FOUND_WHILE_WAITING_FOR_EXECUTION_1_TO_FINISH,
                                        processInstanceId,
                                        activeLeafExecutions);
        }
    }

    public boolean isProcessInstanceAtReceiveTask(String processInstanceId) {
        List<Execution> executionsAtReceiveTask = findExecutionsAtReceiveTask(processInstanceId);
        return !executionsAtReceiveTask.isEmpty();
    }

    public List<Execution> findExecutionsAtReceiveTask(String processInstanceId) {
        List<Execution> allProcessExecutions = getProcessExecutionsWhichExecuteCallActivity(processInstanceId);
        return allProcessExecutions.stream()
                                   .filter(execution -> !findCurrentActivitiesAtReceiveTask(execution).isEmpty())
                                   .collect(Collectors.toList());
    }

    public List<Execution> getProcessExecutionsWhichExecuteCallActivity(String processInstanceId) {
        List<Execution> allProcessExecutions = getAllProcessExecutions(processInstanceId);
        return allProcessExecutions.stream()
                                   .filter(e -> e.getActivityId() != null)
                                   .collect(Collectors.toList());
    }

    public List<Execution> getLeafExecutionsByFilter(String processInstanceId, Predicate<? super ExecutionEntityImpl> executionFilter) {
        List<Execution> allExecutions = getAllProcessExecutions(processInstanceId);
        return allExecutions.stream()
                            .filter(execution -> isNotParent(allExecutions, execution))
                            .map(FlowableFacade::toExecutionEntityImpl)
                            .filter(executionFilter)
                            .collect(Collectors.toList());
    }

    private List<Execution> getAllExecutionsWithoutChildren(String processInstanceId) {
        List<Execution> allExecutions = getAllProcessExecutions(processInstanceId);
        return allExecutions.stream()
                            .filter(execution -> isNotParent(allExecutions, execution))
                            .collect(Collectors.toList());
    }

    private boolean doesExecutionHaveDeadLetterJobs(Execution execution) {
        return !getDeadLetterJobsByProcessInstanceId(execution).isEmpty();
    }

    private boolean isNotParent(List<Execution> allExecutions, Execution execution) {
        for (Execution exec : allExecutions) {
            if (Objects.equals(execution.getId(), exec.getParentId()) || Objects.equals(execution.getId(), exec.getSuperExecutionId())) {
                return false;
            }
        }
        return true;
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
        LOGGER.debug(format(PROCESS_0_HAS_BEEN_SUSPENDED_SUCCESSFULLY, processInstanceId));
    }

    private void abortProcessInstance(String processInstanceId, String deleteReason) {
        try {
            processEngine.getRuntimeService()
                         .deleteProcessInstance(processInstanceId, deleteReason);
            LOGGER.debug(format(PROCESS_0_HAS_BEEN_DELETED_SUCCESSFULLY, processInstanceId));
        } catch (FlowableOptimisticLockingException e) {
            LOGGER.error(MessageFormat.format(ABORT_OPERATION_FOR_PROCESS_0_FAILED, processInstanceId), e);
        }
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

    public static ExecutionEntityImpl toExecutionEntityImpl(Execution execution) {
        return (ExecutionEntityImpl) execution;
    }
}

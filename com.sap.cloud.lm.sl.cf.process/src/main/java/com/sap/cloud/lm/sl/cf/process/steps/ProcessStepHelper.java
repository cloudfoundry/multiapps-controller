package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.MessageFormat;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.JobQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.model.ErrorType;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.cf.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProviderFactory;
import com.sap.cloud.lm.sl.cf.persistence.services.ProgressMessageService;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;

public class ProcessStepHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessStepHelper.class);
    private static final String CORRELATION_ID = "correlationId";

    private ProgressMessageService progressMessageService;
    private ProcessLoggerProviderFactory processLoggerProviderFactory;
    private TaskIndexProvider taskIndexProvider;

    String taskId;
    String taskIndex;
    private boolean isInError;

    public ProcessStepHelper(ProgressMessageService progressMessageService, ProcessLoggerProviderFactory processLoggerProviderFactory,
        TaskIndexProvider stepIndexProvider) {
        this.progressMessageService = progressMessageService;
        this.processLoggerProviderFactory = processLoggerProviderFactory;
        this.taskIndexProvider = stepIndexProvider;
    }

    protected void postExecuteStep(DelegateExecution context, StepPhase state) {
        logDebug(context, MessageFormat.format(Messages.STEP_FINISHED, context.getCurrentActivityName()));

        processLoggerProviderFactory.removeAll();
        // Write the log messages:
        try {
            processLoggerProviderFactory.appendToDefaultDir(context);
        } catch (IOException | FileStorageException e) {
            LOGGER.warn(MessageFormat.format(Messages.COULD_NOT_PERSIST_LOGS_FILE, e.getMessage()), e);
        }

        context.setVariable(Constants.VAR_STEP_EXECUTION, state.toString());
    }

    void preExecuteStep(DelegateExecution context, StepPhase initialPhase) throws SLException {
        init(context, initialPhase);

        context.setVariable(Constants.TASK_ID, taskId);
        context.setVariable(Constants.TASK_INDEX, taskIndex);

        if (isInError) {
            deletePreviousExecutionData(context);
        }
        logTaskStartup(context);
    }

    private void init(DelegateExecution context, StepPhase initialPhase) {
        this.isInError = isInError(context);
        this.taskId = context.getCurrentActivityId();
        this.taskIndex = Integer.toString(computeTaskIndex(context, initialPhase, isInError));
    }

    private boolean isInError(DelegateExecution context) {
        Job job = getJob(context);
        if (job == null) {
            return false;
        }
        String exceptionMessage = job.getExceptionMessage();
        return exceptionMessage != null && !exceptionMessage.isEmpty();
    }

    Job getJob(DelegateExecution context) {
        JobQuery jobQuery = context.getEngineServices()
            .getManagementService()
            .createJobQuery();
        if (jobQuery == null) {
            return null;
        }
        return jobQuery.processInstanceId(context.getProcessInstanceId())
            .singleResult();
    }

    private int computeTaskIndex(DelegateExecution context, StepPhase initialPhase, boolean isInError) {
        int taskIndex = getLastTaskIndex(context);
        if (!isInError && !initialPhase.equals(StepPhase.RETRY) && !initialPhase.equals(StepPhase.POLL)) {
            return ++taskIndex;
        }
        return taskIndex;
    }

    private int getLastTaskIndex(DelegateExecution context) throws SLException {
        String taskId = context.getCurrentActivityId();
        String lastTaskExecutionId = progressMessageService.findLastTaskExecutionId(getCorrelationId(context), taskId);
        if (lastTaskExecutionId == null) {
            return taskIndexProvider.getTaskIndex(context);
        }
        return Integer.parseInt(lastTaskExecutionId);
    }

    private String getCorrelationId(DelegateExecution context) {
        return (String) context.getVariable(CORRELATION_ID);
    }

    private void logTaskStartup(DelegateExecution context) {
        String message = MessageFormat.format(Messages.EXECUTING_TASK, context.getCurrentActivityId(), context.getId());
        progressMessageService.add(new ProgressMessage(getCorrelationId(context), taskId, taskIndex, ProgressMessageType.TASK_STARTUP,
            message, new Timestamp(System.currentTimeMillis())));
    }

    protected void deletePreviousExecutionData(DelegateExecution context) {
        progressMessageService.removeByProcessIdTaskIdAndTaskExecutionId(getCorrelationId(context), taskId, taskIndex);
        if (context.hasVariable(Constants.RETRY_TASK_ID)) {
            String retryTaskId = (String) context.getVariable(Constants.RETRY_TASK_ID);
            progressMessageService.removeByProcessIdTaskIdAndTaskExecutionId(getCorrelationId(context), retryTaskId, taskIndex);
            context.removeVariable(Constants.RETRY_TASK_ID);
        }
        String processId = context.getProcessInstanceId();
        ErrorType errorType = StepsUtil.getErrorType(context);
        if (errorType == null) {
            return;
        }
        LOGGER.debug(MessageFormat.format(Messages.DELETING_ERROR_TYPE_O_FOR_PROCESS_1, errorType, processId));
        context.removeVariable(Constants.VAR_ERROR_TYPE);
    }

    protected void logException(DelegateExecution context, Throwable t) {
        LOGGER.error(Messages.EXCEPTION_CAUGHT, t);
        getLogger(context).error(Messages.EXCEPTION_CAUGHT, t);

        storeExceptionInProgressMessageService(context, t);

        if (t instanceof ContentException) {
            StepsUtil.setErrorType(context, ErrorType.CONTENT_ERROR);
        } else {
            StepsUtil.setErrorType(context, ErrorType.UNKNOWN_ERROR);
        }
    }

    public void storeExceptionInProgressMessageService(DelegateExecution context, Throwable t) {
        try {
            ProgressMessage msg = new ProgressMessage(getCorrelationId(context), taskId, taskIndex, ProgressMessageType.ERROR,
                MessageFormat.format(Messages.UNEXPECTED_ERROR, t.getMessage()), new Timestamp(System.currentTimeMillis()));
            progressMessageService.add(msg);
        } catch (SLException e) {
            getLogger(context).error(Messages.SAVING_ERROR_MESSAGE_FAILED, e);
        }
    }

    private void logDebug(DelegateExecution context, String message) {
        getLogger(context).debug(message);
    }

    org.apache.log4j.Logger getLogger(DelegateExecution context) {
        return processLoggerProviderFactory.getDefaultLoggerProvider()
            .getLogger(getCorrelationId(context), this.getClass()
                .getName());
    }

    public void failStepIfProcessIsAborted(DelegateExecution context) throws SLException {
        Boolean processAborted = (Boolean) context.getVariable(Constants.PROCESS_ABORTED);
        if (processAborted != null && processAborted) {
            throw new SLException(Messages.PROCESS_WAS_ABORTED);
        }
    }

}

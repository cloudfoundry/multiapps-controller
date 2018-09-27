package com.sap.cloud.lm.sl.cf.process.steps;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.runtime.Execution;
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

    private ProcessEngineConfiguration processEngineConfiguration;

    String taskId;
    String taskIndex;

    public ProcessStepHelper(ProgressMessageService progressMessageService, ProcessLoggerProviderFactory processLoggerProviderFactory,
        TaskIndexProvider stepIndexProvider, ProcessEngineConfiguration processEngineConfigurationSupplier) {
        this.progressMessageService = progressMessageService;
        this.processLoggerProviderFactory = processLoggerProviderFactory;
        this.taskIndexProvider = stepIndexProvider;
        this.processEngineConfiguration = processEngineConfigurationSupplier;
    }

    protected void postExecuteStep(DelegateExecution context, StepPhase state) {
        logDebug(context, MessageFormat.format(Messages.STEP_FINISHED, context.getCurrentFlowElement()
            .getName()));

        processLoggerProviderFactory.removeAll();
        // Write the log messages:
        try {
            processLoggerProviderFactory.append(context);
        } catch (IOException | FileStorageException e) {
            LOGGER.warn(MessageFormat.format(Messages.COULD_NOT_PERSIST_LOGS_FILE, e.getMessage()), e);
        }

        context.setVariable(Constants.VAR_STEP_EXECUTION, state.toString());
    }

    void preExecuteStep(DelegateExecution context, StepPhase initialPhase) {
        init(context, initialPhase);

        context.setVariable(Constants.TASK_ID, taskId);
        context.setVariable(Constants.TASK_INDEX, taskIndex);

        deletePreviousExecutionData(context);
        logTaskStartup(context);
    }

    private void init(DelegateExecution context, StepPhase initialPhase) {
        this.taskId = context.getCurrentActivityId();
        this.taskIndex = Integer.toString(computeTaskIndex(context, initialPhase));
    }

    private int computeTaskIndex(DelegateExecution context, StepPhase initialPhase) {
        int taskIndex = getLastTaskIndex(context);
        if (!initialPhase.equals(StepPhase.RETRY) && !initialPhase.equals(StepPhase.POLL)) {
            return taskIndex + 1;
        }
        return taskIndex;
    }

    private int getLastTaskIndex(DelegateExecution context) {
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
            ProgressMessage msg = new ProgressMessage(getCorrelationId(context), getCurrentActivityId(context), taskIndex,
                ProgressMessageType.ERROR, MessageFormat.format(Messages.UNEXPECTED_ERROR, t.getMessage()),
                new Timestamp(System.currentTimeMillis()));
            progressMessageService.add(msg);
        } catch (SLException e) {
            getLogger(context).error(Messages.SAVING_ERROR_MESSAGE_FAILED, e);
        }
    }

    // This method is needed because sometimes the DelegateExecution::getCurrentActivityId returns null
    // Check the issue: https://github.com/flowable/flowable-engine/issues/1280
    private String getCurrentActivityId(DelegateExecution context) {
        List<Execution> processExecutions = processEngineConfiguration.getRuntimeService()
            .createExecutionQuery()
            .processInstanceId(context.getProcessInstanceId())
            .list();
        List<Execution> processExecutionsWithActivityIds = processExecutions.stream()
            .filter(e -> e.getActivityId() != null)
            .collect(Collectors.toList());
        if (processExecutionsWithActivityIds.isEmpty()) {
            // if this happen then there is a really big problem with Flowable :)
            throw new SLException("There are no executions for process with id: " + context.getProcessInstanceId());
        }
        return processExecutionsWithActivityIds.get(0)
            .getActivityId();
    }

    private void logDebug(DelegateExecution context, String message) {
        getLogger(context).debug(message);
    }

    org.apache.log4j.Logger getLogger(DelegateExecution context) {
        return processLoggerProviderFactory.getDefaultLoggerProvider()
            .getLogger(getCorrelationId(context), this.getClass()
                .getName());
    }

    public void failStepIfProcessIsAborted(DelegateExecution context) {
        Boolean processAborted = (Boolean) context.getVariable(Constants.PROCESS_ABORTED);
        if (processAborted != null && processAborted) {
            throw new SLException(Messages.PROCESS_WAS_ABORTED);
        }
    }

}

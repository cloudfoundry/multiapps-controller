package com.sap.cloud.lm.sl.cf.process.steps;

import static java.text.MessageFormat.format;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.MessageFormat;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.runtime.Job;
import org.activiti.engine.runtime.JobQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.dao.ContextExtensionDao;
import com.sap.cloud.lm.sl.cf.core.model.ContextExtension;
import com.sap.cloud.lm.sl.cf.core.model.ErrorType;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.model.ProgressMessage;
import com.sap.cloud.lm.sl.persistence.model.ProgressMessage.ProgressMessageType;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.persistence.services.ProcessLoggerProviderFactory;
import com.sap.cloud.lm.sl.persistence.services.ProgressMessageService;

public class ProcessStepHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessStepHelper.class);
    private static final String CORRELATION_ID = "correlationId";

    private ContextExtensionDao contextExtensionDao;
    private ProgressMessageService progressMessageService;
    protected ProcessLoggerProviderFactory processLoggerProviderFactory;
    private StepIndexProvider stepIndexProvider;

    private String indexedStepName;
    private int stepIndex;
    private boolean isInError;

    public ProcessStepHelper(ProgressMessageService progressMessageService, ProcessLoggerProviderFactory processLoggerProviderFactory,
        StepIndexProvider stepIndexProvider, ContextExtensionDao contextExtensionDao) {
        this.progressMessageService = progressMessageService;
        this.processLoggerProviderFactory = processLoggerProviderFactory;
        this.stepIndexProvider = stepIndexProvider;
        this.contextExtensionDao = contextExtensionDao;
    }

    protected void postExecuteStep(DelegateExecution context, StepPhase state) {
        logDebug(context, MessageFormat.format(Messages.STEP_FINISHED, context.getCurrentActivityName()));

        processLoggerProviderFactory.removeAll();
        // Write the log messages:
        try {
            processLoggerProviderFactory.append(context, ProcessLoggerProviderFactory.LOG_DIR);
        } catch (IOException | FileStorageException e) {
            LOGGER.warn(MessageFormat.format(Messages.COULD_NOT_PERSIST_LOGS_FILE, e.getMessage()), e);
        }

        context.setVariable(Constants.VAR_STEP_EXECUTION, state.toString());
    }

    void preExecuteStep(DelegateExecution context, StepPhase initialPhase) throws SLException {
        init(context, initialPhase);

        context.setVariable(Constants.INDEXED_STEP_NAME, indexedStepName);

        if (isInError) {
            deletePreviousExecutionData(context);
        }
        logTaskStartup(context, indexedStepName);
    }

    private void init(DelegateExecution context, StepPhase initialPhase) {
        this.isInError = isInError(context);
        this.stepIndex = computeStepIndex(context, initialPhase, isInError);
        this.indexedStepName = context.getCurrentActivityId() + stepIndex;
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
        JobQuery jobQuery = context.getEngineServices().getManagementService().createJobQuery();
        if (jobQuery == null) {
            return null;
        }
        return jobQuery.processInstanceId(context.getProcessInstanceId()).singleResult();
    }

    private int computeStepIndex(DelegateExecution context, StepPhase initialPhase, boolean isInError) {
        int stepIndex = getLastStepIndex(context);
        if (!isInError && !initialPhase.equals(StepPhase.RETRY) && !initialPhase.equals(StepPhase.POLL)
            && !initialPhase.equals(StepPhase.WAIT)) {
            return ++stepIndex;
        }
        return stepIndex;
    }

    private int getLastStepIndex(DelegateExecution context) throws SLException {
        String activityId = context.getCurrentActivityId();
        String lastTaskId = progressMessageService.findLastTaskId(getCorrelationId(context), activityId);
        if (lastTaskId == null) {
            return stepIndexProvider.getStepIndex(context);
        }
        return Integer.parseInt(lastTaskId.substring(activityId.length()));
    }

    private String getCorrelationId(DelegateExecution context) {
        return (String) context.getVariable(CORRELATION_ID);
    }

    private void logTaskStartup(DelegateExecution context, String indexedStepName) {
        String message = format(Messages.EXECUTING_ACTIVITI_TASK, context.getId(), context.getCurrentActivityId());
        progressMessageService.add(new ProgressMessage(getCorrelationId(context), indexedStepName, ProgressMessageType.TASK_STARTUP,
            message, new Timestamp(System.currentTimeMillis())));
    }

    protected void deletePreviousExecutionData(DelegateExecution context) {
        progressMessageService.removeByProcessIdAndTaskId(getCorrelationId(context), indexedStepName);
        if (context.hasVariable(Constants.RETRY_STEP_NAME)) {
            String taskId = (String) context.getVariable(Constants.RETRY_STEP_NAME) + stepIndex;
            progressMessageService.removeByProcessIdAndTaskId(getCorrelationId(context), taskId);
            context.removeVariable(Constants.RETRY_STEP_NAME);
        }
        String processId = context.getProcessInstanceId();
        ContextExtension contextExtension = contextExtensionDao.find(processId, Constants.VAR_ERROR_TYPE);
        if (contextExtension == null) {
            return;
        }
        LOGGER.debug(MessageFormat.format(Messages.DELETING_CONTEXT_EXTENSION_WITH_ID_NAME_AND_VALUE_FOR_PROCESS, contextExtension.getId(),
            contextExtension.getName(), contextExtension.getValue(), processId));
        contextExtensionDao.remove(contextExtension.getId());
    }

    protected void logException(DelegateExecution context, Throwable t) {
        LOGGER.error(Messages.EXCEPTION_CAUGHT, t);
        getLogger(context).error(Messages.EXCEPTION_CAUGHT, t);

        storeExceptionInProgressMessageService(context, t);

        if (t instanceof ContentException) {
            StepsUtil.setErrorType(context.getProcessInstanceId(), contextExtensionDao, ErrorType.CONTENT_ERROR);
        } else {
            StepsUtil.setErrorType(context.getProcessInstanceId(), contextExtensionDao, ErrorType.UNKNOWN_ERROR);
        }
    }

    public void storeExceptionInProgressMessageService(DelegateExecution context, Throwable t) {
        try {
            ProgressMessage msg = new ProgressMessage(getCorrelationId(context), indexedStepName, ProgressMessageType.ERROR,
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
        return processLoggerProviderFactory.getDefaultLoggerProvider().getLogger(getCorrelationId(context), this.getClass().getName());
    }

    public void failStepIfProcessIsAborted(DelegateExecution context) throws SLException {
        Boolean processAborted = (Boolean) context.getVariable(Constants.PROCESS_ABORTED);
        if (processAborted != null && processAborted) {
            throw new SLException(Messages.PROCESS_WAS_ABORTED);
        }
    }

}

package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Inject;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.dao.ProgressMessageDao;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLogger;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProvider;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLogsPersister;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.common.SLException;

public abstract class AbstractProcessExecutionListener implements ExecutionListener {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProcessExecutionListener.class);

    @Inject
    private ProgressMessageDao progressMessageDao;
    @Inject
    private StepLogger.Factory stepLoggerFactory;
    @Inject
    private ProcessLoggerProvider processLoggerProvider;
    @Inject
    private ProcessLogsPersister processLogsPersister;

    private StepLogger stepLogger;

    @Override
    public void notify(DelegateExecution context) {
        try {
            this.stepLogger = createStepLogger(context);
            String correlationId = StepsUtil.getCorrelationId(context);
            if (correlationId == null) {
                correlationId = context.getProcessInstanceId();
                context.setVariable(com.sap.cloud.lm.sl.cf.process.Constants.VAR_CORRELATION_ID, correlationId);
            }

            correlationId = StepsUtil.getCorrelationId(context);
            String processInstanceId = context.getProcessInstanceId();

            // Check if this is subprocess
            if (!processInstanceId.equals(correlationId)) {
                return;
            }

            notifyInternal(context);
        } catch (Exception e) {
            logException(context, e, Messages.EXECUTION_OF_PROCESS_LISTENER_HAS_FAILED);
            throw new SLException(e, Messages.EXECUTION_OF_PROCESS_LISTENER_HAS_FAILED);
        } finally {
            finalizeLogs(context);
        }
    }

    protected void logException(DelegateExecution context, Exception e, String message) {
        LOGGER.error(message, e);
        getProcessLogger().error(message, e);
    }

    protected ProcessLogger getProcessLogger() {
        return getStepLogger().getProcessLogger();
    }

    protected void finalizeLogs(DelegateExecution context) {
        processLogsPersister.persistLogs(getCorrelationId(context), getTaskId(context));
    }

    private String getCorrelationId(DelegateExecution context) {
        return (String) context.getVariable(com.sap.cloud.lm.sl.cf.persistence.message.Constants.CORRELATION_ID);
    }

    private String getTaskId(DelegateExecution context) {
        String taskId = (String) context.getVariable(Constants.TASK_ID);
        return taskId != null ? taskId : context.getCurrentActivityId();
    }
    
    protected StepLogger getStepLogger() {
        if (stepLogger == null) {
            throw new IllegalStateException(Messages.STEP_LOGGER_NOT_INITIALIZED);
        }
        return stepLogger;
    }

    private StepLogger createStepLogger(DelegateExecution context) {
        return stepLoggerFactory.create(context, progressMessageDao, processLoggerProvider, getLogger());
    }

    protected abstract void notifyInternal(DelegateExecution context) throws Exception;

    protected abstract org.slf4j.Logger getLogger();

}

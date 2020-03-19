package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Inject;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.persistence.service.ProgressMessageService;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLogger;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProvider;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLogsPersister;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.cf.process.util.HistoricOperationEventPersister;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.common.SLException;

public abstract class AbstractProcessExecutionListener implements ExecutionListener {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProcessExecutionListener.class);

    @Inject
    private ProgressMessageService progressMessageService;
    @Inject
    private StepLogger.Factory stepLoggerFactory;
    @Inject
    private ProcessLoggerProvider processLoggerProvider;
    @Inject
    private ProcessLogsPersister processLogsPersister;
    @Inject
    private HistoricOperationEventPersister historicOperationEventPersister;

    private StepLogger stepLogger;

    @Override
    public void notify(DelegateExecution execution) {
        try {
            String correlationId = StepsUtil.getCorrelationId(execution);
            if (correlationId == null) {
                correlationId = execution.getProcessInstanceId();
                execution.setVariable(Constants.VAR_CORRELATION_ID, correlationId);
            }
            this.stepLogger = createStepLogger(execution);
            notifyInternal(execution);
        } catch (Exception e) {
            logException(e, Messages.EXECUTION_OF_PROCESS_LISTENER_HAS_FAILED);
            throw new SLException(e, Messages.EXECUTION_OF_PROCESS_LISTENER_HAS_FAILED);
        } finally {
            finalizeLogs(execution);
        }
    }

    protected void logException(Exception e, String message) {
        LOGGER.error(message, e);
        getProcessLogger().error(message, e);
    }

    protected ProcessLogger getProcessLogger() {
        return getStepLogger().getProcessLogger();
    }

    protected void finalizeLogs(DelegateExecution execution) {
        processLogsPersister.persistLogs(StepsUtil.getCorrelationId(execution), getTaskId(execution));
    }

    private String getTaskId(DelegateExecution execution) {
        String taskId = (String) execution.getVariable(Constants.TASK_ID);
        return taskId != null ? taskId : execution.getCurrentActivityId();
    }

    protected StepLogger getStepLogger() {
        if (stepLogger == null) {
            throw new IllegalStateException(Messages.STEP_LOGGER_NOT_INITIALIZED);
        }
        return stepLogger;
    }

    protected HistoricOperationEventPersister getHistoricOperationEventPersister() {
        return historicOperationEventPersister;
    }

    private StepLogger createStepLogger(DelegateExecution execution) {
        return stepLoggerFactory.create(execution, progressMessageService, processLoggerProvider, getLogger());
    }

    protected boolean isRootProcess(DelegateExecution execution) {
        String correlationId = StepsUtil.getCorrelationId(execution);
        String processInstanceId = execution.getProcessInstanceId();
        return processInstanceId.equals(correlationId);
    }

    protected abstract void notifyInternal(DelegateExecution execution) throws Exception;

    protected abstract org.slf4j.Logger getLogger();

}

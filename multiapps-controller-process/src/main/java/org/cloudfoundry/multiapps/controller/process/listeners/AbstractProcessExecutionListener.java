package org.cloudfoundry.multiapps.controller.process.listeners;

import javax.inject.Inject;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.persistence.service.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogger;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogsPersister;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.HistoricOperationEventPersister;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            String correlationId = VariableHandling.get(execution, Variables.CORRELATION_ID);
            if (correlationId == null) {
                correlationId = execution.getProcessInstanceId();
                VariableHandling.set(execution, Variables.CORRELATION_ID, correlationId);
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
        processLogsPersister.persistLogs(VariableHandling.get(execution, Variables.CORRELATION_ID), getTaskId(execution));
    }

    private String getTaskId(DelegateExecution execution) {
        String taskId = VariableHandling.get(execution, Variables.TASK_ID);
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
        String correlationId = VariableHandling.get(execution, Variables.CORRELATION_ID);
        String processInstanceId = execution.getProcessInstanceId();
        return processInstanceId.equals(correlationId);
    }

    protected abstract void notifyInternal(DelegateExecution execution) throws Exception;

    protected Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }

}

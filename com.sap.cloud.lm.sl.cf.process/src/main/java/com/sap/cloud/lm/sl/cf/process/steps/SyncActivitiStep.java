package com.sap.cloud.lm.sl.cf.process.steps;

import javax.inject.Inject;
import javax.inject.Named;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.cloudfoundry.client.lib.CloudFoundryException;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.dao.ContextExtensionDao;
import com.sap.cloud.lm.sl.cf.process.exception.MonitoringException;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.services.AbstractFileService;
import com.sap.cloud.lm.sl.persistence.services.ProcessLoggerProviderFactory;
import com.sap.cloud.lm.sl.persistence.services.ProgressMessageService;

public abstract class SyncActivitiStep implements TaskIndexProvider, JavaDelegate {

    protected final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(getClass());

    @Inject
    protected CloudFoundryClientProvider clientProvider;
    @Inject
    protected ContextExtensionDao contextExtensionDao;
    @Inject
    private StepLogger.Factory stepLoggerFactory;
    @Inject
    protected ProcessLoggerProviderFactory processLoggerProviderFactory;
    @Inject
    protected ProgressMessageService progressMessageService;
    @Inject
    @Named("fileService")
    protected AbstractFileService fileService;
    protected ProcessStepHelper stepHelper;
    private StepLogger stepLogger;

    @Override
    public void execute(DelegateExecution context) throws Exception {
        StepPhase stepPhase = null;
        createStepLogger(context);
        ExecutionWrapper executionWrapper = createExecutionWrapper(context);
        try {
            MDC.put(Constants.ATTR_CORRELATION_ID, StepsUtil.getCorrelationId(context));
            getStepHelper().preExecuteStep(context, getInitialStepPhase(executionWrapper));
            stepPhase = executeStep(executionWrapper);
            getStepHelper().failStepIfProcessIsAborted(context);
            LOGGER.debug("Execution finished");
        } catch (MonitoringException | CloudFoundryException e) {
            getStepLogger().errorWithoutProgressMessage(e.getMessage());
            stepPhase = getResultStepPhase();
            handleException(context, e);
        } catch (Throwable t) {
            stepPhase = StepPhase.RETRY;
            handleException(context, t);
        } finally {
            StepsUtil.setStepPhase(executionWrapper, stepPhase);
            postExecuteStep(context, stepPhase);
        }
    }

    protected StepPhase getResultStepPhase() {
        return StepPhase.RETRY;
    }

    protected StepPhase getInitialStepPhase(ExecutionWrapper executionWrapper) {
        return StepPhase.EXECUTE;
    }

    protected ExecutionWrapper createExecutionWrapper(DelegateExecution context) {
        return new ExecutionWrapper(context, contextExtensionDao, stepLogger, clientProvider, processLoggerProviderFactory);
    }

    private void handleException(DelegateExecution context, Throwable t) throws Exception {
        t = getWithProperMessage(t);
        getStepHelper().logException(context, t);
        throw t instanceof Exception ? (Exception) t : new Exception(t);
    }

    protected void postExecuteStep(DelegateExecution context, StepPhase stepState) {
        try {
            getStepHelper().postExecuteStep(context, stepState);
        } catch (SLException e) {
            getStepHelper().storeExceptionInProgressMessageService(context, e);
            logException(context, e);
            throw e;
        }
    }

    protected abstract StepPhase executeStep(ExecutionWrapper execution) throws Exception;

    protected StepLogger getStepLogger() {
        if (stepLogger == null) {
            throw new IllegalStateException(Messages.STEP_LOGGER_NOT_INITIALIZED);
        }
        return stepLogger;
    }

    protected void createStepLogger(DelegateExecution context) {
        stepLogger = stepLoggerFactory.create(context, progressMessageService, processLoggerProviderFactory, LOGGER);
    }

    protected Throwable getWithProperMessage(Throwable t) {
        if (t.getMessage() == null || t.getMessage()
            .isEmpty()) {
            return new Exception("An unknown error occurred", t);
        }
        return t;
    }

    public void logException(DelegateExecution context, Throwable t) {
        getStepHelper().logException(context, t);
    }

    protected ProcessStepHelper getStepHelper() {
        if (stepHelper == null) {
            stepHelper = new ProcessStepHelper(getProgressMessageService(), getProcessLoggerProvider(), this, contextExtensionDao);
        }
        return stepHelper;
    }

    protected ProcessLoggerProviderFactory getProcessLoggerProvider() {
        return processLoggerProviderFactory;
    }

    protected ProgressMessageService getProgressMessageService() {
        return progressMessageService;
    }

    @Override
    public int getTaskIndex(DelegateExecution context) {
        return (getIndexVariable() != null ? (int) context.getVariable(getIndexVariable()) : 0) - 1;
    }

    protected String getIndexVariable() {
        return null;
    }

}

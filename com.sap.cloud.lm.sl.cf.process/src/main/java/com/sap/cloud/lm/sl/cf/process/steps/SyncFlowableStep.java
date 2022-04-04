package com.sap.cloud.lm.sl.cf.process.steps;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProvider;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLogsPersister;
import com.sap.cloud.lm.sl.cf.persistence.services.ProgressMessageService;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.common.SLException;

public abstract class SyncFlowableStep implements JavaDelegate {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    protected CloudControllerClientProvider clientProvider;
    @Inject
    @Named("fileService")
    protected FileService fileService;
    protected ProcessStepHelper stepHelper;
    @Inject
    private StepLogger.Factory stepLoggerFactory;
    @Inject
    private ProgressMessageService progressMessageService;
    @Inject
    private ProcessEngineConfiguration processEngineConfiguration;
    @Inject
    private ProcessLoggerProvider processLoggerProvider;
    @Inject
    private ProcessLogsPersister processLogsPersister;
    private StepLogger stepLogger;

    @Override
    public void execute(DelegateExecution context) {
        initializeStepLogger(context);
        ExecutionWrapper executionWrapper = createExecutionWrapper(context);
        StepPhase stepPhase = getInitialStepPhase(executionWrapper);
        try {
            MDC.put(Constants.ATTR_CORRELATION_ID, StepsUtil.getCorrelationId(context));
            getStepHelper().preExecuteStep(context, stepPhase);
            stepPhase = executeStep(executionWrapper);
            if (stepPhase == StepPhase.RETRY) {
                throw new SLException("A step of the process has failed. Retrying it may solve the issue.");
            }
            getStepHelper().failStepIfProcessIsAborted(context);
        } catch (Throwable t) {
            stepPhase = StepPhase.RETRY;
            handleException(context, t);
        } finally {
            StepsUtil.setStepPhase(context, stepPhase);
            postExecuteStep(context, stepPhase);
        }
    }

    protected StepPhase getInitialStepPhase(ExecutionWrapper executionWrapper) {
        return StepPhase.EXECUTE;
    }

    protected ExecutionWrapper createExecutionWrapper(DelegateExecution context) {
        return new ExecutionWrapper(context, stepLogger, clientProvider);
    }

    private void handleException(DelegateExecution context, Throwable t) {
        t = getWithProperMessage(t);
        getStepHelper().logException(context, t);
        throw t instanceof RuntimeException ? (RuntimeException) t : new RuntimeException(t);
    }

    protected void postExecuteStep(DelegateExecution context, StepPhase stepState) {
        try {
            getStepHelper().postExecuteStep(context, stepState);
        } catch (SLException e) {
            getStepHelper().logException(context, e);
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

    protected void initializeStepLogger(DelegateExecution context) {
        stepLogger = stepLoggerFactory.create(context, progressMessageService, processLoggerProvider, logger);
    }

    protected Throwable getWithProperMessage(Throwable t) {
        if (t.getMessage() == null || t.getMessage()
                                       .isEmpty()) {
            return new Exception("An unknown error occurred", t);
        }
        return t;
    }

    protected ProcessStepHelper getStepHelper() {
        if (stepHelper == null) {
            stepHelper = new ProcessStepHelper(getProgressMessageService(),
                                               getStepLogger(),
                                               getProcessLogsPersister(),
                                               processEngineConfiguration);
        }
        return stepHelper;
    }

    protected ProgressMessageService getProgressMessageService() {
        return progressMessageService;
    }

    protected ProcessLogsPersister getProcessLogsPersister() {
        return processLogsPersister;
    }

}

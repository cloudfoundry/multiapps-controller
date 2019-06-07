package com.sap.cloud.lm.sl.cf.process.steps;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudServiceBrokerException;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
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
    private StepLogger.Factory stepLoggerFactory;
    @Inject
    private ProgressMessageService progressMessageService;
    @Inject
    @Named("fileService")
    protected FileService fileService;
    @Inject
    private ProcessEngineConfiguration processEngineConfiguration;
    @Inject
    private ProcessLoggerProvider processLoggerProvider;
    @Inject
    private ProcessLogsPersister processLogsPersister;
    protected ProcessStepHelper stepHelper;
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
        } catch (Exception e) {
            stepPhase = StepPhase.RETRY;
            handleException(executionWrapper, e);
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

    protected void handleException(ExecutionWrapper execution, Exception e) {
        try {
            e = preprocessException(e);
            onError(execution, e);
        } catch (Exception ex) {
            ex = getWithProperMessage(ex);
            getStepHelper().logException(execution.getContext(), ex);
            throw ex instanceof RuntimeException ? (RuntimeException) ex : new RuntimeException(ex);
        }
    }

    protected void postExecuteStep(DelegateExecution context, StepPhase stepState) {
        try {
            getStepHelper().postExecuteStep(context, stepState);
        } catch (SLException e) {
            getStepHelper().logException(context, e);
            throw e;
        }
    }
    

    protected void onError(ExecutionWrapper execution, Exception e) throws Exception {
        onStepError(execution.getContext(), e);
    }

    private Exception preprocessException(Exception e) {
        if (e instanceof CloudOperationException && !(e instanceof CloudServiceBrokerException)) {
            e = new CloudControllerException((CloudOperationException) e);
        } else if (e instanceof FileStorageException) {
            e = new SLException(e, e.getMessage());
        }
        return e;
    }

    protected abstract StepPhase executeStep(ExecutionWrapper execution) throws Exception;

    /**
     * <p>
     * Add step specific progress message. Eventually, re-throw the exception. Example:
     * 
     * <pre>
     * <code>
     * protected void onError(DelegateExecution context, Exception e) throws Exception {
     *     getStepLogger().error(e, "You passed invalid MTA archive format");
     *     throw e;
     *  }
     * </code>
     * </pre>
     * </p>
     * 
     * @param context flowable context of the step
     * @param e thrown exception from {@link #executeStep(ExecutionWrapper) executeStep} and pre-processed by
     *        {@link #handleException(DelegateExecution, Exception) handleException}
     */
    protected abstract void onStepError(DelegateExecution context, Exception e) throws Exception;

    protected StepLogger getStepLogger() {
        if (stepLogger == null) {
            throw new IllegalStateException(Messages.STEP_LOGGER_NOT_INITIALIZED);
        }
        return stepLogger;
    }

    protected void initializeStepLogger(DelegateExecution context) {
        stepLogger = stepLoggerFactory.create(context, progressMessageService, processLoggerProvider, logger);
    }

    protected Exception getWithProperMessage(Exception e) {
        if (e.getMessage() == null || e.getMessage()
            .isEmpty()) {
            return new Exception("An unknown error occurred", e);
        }
        return e;
    }

    protected ProcessStepHelper getStepHelper() {
        if (stepHelper == null) {
            stepHelper = new ProcessStepHelper(getProgressMessageService(), getStepLogger(), getProcessLogsPersister(),
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

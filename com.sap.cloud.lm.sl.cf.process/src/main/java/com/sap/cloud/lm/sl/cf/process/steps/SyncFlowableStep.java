package com.sap.cloud.lm.sl.cf.process.steps;

import java.util.function.BiFunction;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
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
import com.sap.cloud.lm.sl.cf.core.dao.ProgressMessageDao;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProvider;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLogsPersister;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;

public abstract class SyncFlowableStep implements JavaDelegate {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    protected CloudControllerClientProvider clientProvider;
    @Inject
    private StepLogger.Factory stepLoggerFactory;
    @Inject
    private ProgressMessageDao progressMessageDao;
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
            handleException(executionWrapper.getContext(), e);
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

    private void handleException(DelegateExecution context, Exception e) {
        try {
            StepPhase stepPhase = StepsUtil.getStepPhase(context);
            if (stepPhase == StepPhase.POLL) {
                throw e;
            }
            onStepError(context, e);
        } catch (Exception ex) {
            ex = getWithProperMessage(ex);
            getStepHelper().logExceptionAndStoreProgressMessage(context, ex);
            throw ex instanceof RuntimeException ? (RuntimeException) ex : new RuntimeException(ex);
        }
    }

    /**
     * <p>
     * Handle exception thrown during {@link #executeStep(ExecutionWrapper) executeStep}
     * </p>
     * <p>
     * Can be overridden if standard behavior does not fulfill custom step requirements. For example, exception can be parsed to other
     * exception or not thrown at all.
     * </p>
     * <p>
     * 
     * @param context flowable context of the step
     * @param e thrown exception from {@link #executeStep(ExecutionWrapper) executeStep} and pre-processed by
     *        {@link #handleException(DelegateExecution, Exception) handleException}
     */
    protected void onStepError(DelegateExecution context, Exception e) throws Exception {
        processException(e, getStepErrorMessage(context));
    }

    protected void processException(Exception e, String detailedMessage) throws Exception {
        e = handleControllerException(e);
        throw getExceptionConstructor(e).apply(e, detailedMessage + ": " + e.getMessage());
    }

    private static Exception handleControllerException(Exception e) {
        if (e instanceof CloudOperationException && !(e instanceof CloudServiceBrokerException)) {
            return new CloudControllerException((CloudOperationException) e);
        }
        return e;
    }

    private static BiFunction<Throwable, String, Exception> getExceptionConstructor(Exception e) {
        if (e instanceof ContentException) {
            return ContentException::new;
        }
        return SLException::new;
    }

    protected void postExecuteStep(DelegateExecution context, StepPhase stepState) {
        try {
            getStepHelper().postExecuteStep(context, stepState);
        } catch (SLException e) {
            getStepHelper().logExceptionAndStoreProgressMessage(context, e);
            throw e;
        }
    }

    protected abstract String getStepErrorMessage(DelegateExecution context);

    protected abstract StepPhase executeStep(ExecutionWrapper execution) throws Exception;

    protected StepLogger getStepLogger() {
        if (stepLogger == null) {
            throw new IllegalStateException(Messages.STEP_LOGGER_NOT_INITIALIZED);
        }
        return stepLogger;
    }

    protected void initializeStepLogger(DelegateExecution context) {
        stepLogger = stepLoggerFactory.create(context, progressMessageDao, processLoggerProvider, logger);
    }

    protected Exception getWithProperMessage(Exception e) {
        if (StringUtils.isEmpty(e.getMessage())) {
            return new Exception("An unknown error occurred", e);
        }
        return e;
    }

    protected ProcessStepHelper getStepHelper() {
        if (stepHelper == null) {
            stepHelper = new ProcessStepHelper(getProgressMessageDao(), getStepLogger(), getProcessLogsPersister(),
                processEngineConfiguration);
        }
        return stepHelper;
    }

    protected ProgressMessageDao getProgressMessageDao() {
        return progressMessageDao;
    }

    protected ProcessLogsPersister getProcessLogsPersister() {
        return processLogsPersister;
    }

}

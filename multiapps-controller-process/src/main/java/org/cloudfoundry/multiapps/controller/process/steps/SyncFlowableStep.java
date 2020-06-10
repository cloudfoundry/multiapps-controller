package org.cloudfoundry.multiapps.controller.process.steps;

import java.util.function.BiFunction;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudServiceBrokerException;
import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.persistence.service.ProgressMessageService;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.LoggingUtil;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLoggerProvider;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogsPersister;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ExceptionMessageTailMapper;
import org.cloudfoundry.multiapps.controller.process.util.ExceptionMessageTailMapper.CloudComponents;
import org.cloudfoundry.multiapps.controller.process.util.StepLogger;
import org.cloudfoundry.multiapps.controller.process.variables.VariableHandling;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SyncFlowableStep implements JavaDelegate {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    protected CloudControllerClientProvider clientProvider;
    @Inject
    private StepLogger.Factory stepLoggerFactory;
    @Inject
    protected ProgressMessageService progressMessageService;
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
    @Inject
    protected ApplicationConfiguration configuration;

    @Override
    public void execute(DelegateExecution execution) {
        LoggingUtil.logWithCorrelationId(VariableHandling.get(execution, Variables.CORRELATION_ID), () -> executeInternal(execution));
    }

    private void executeInternal(DelegateExecution execution) {
        initializeStepLogger(execution);
        ProcessContext context = createProcessContext(execution);
        StepPhase stepPhase = getInitialStepPhase(context);
        try {
            getStepHelper().preExecuteStep(context, stepPhase);
            stepPhase = executeStep(context);
            if (stepPhase == StepPhase.RETRY) {
                throw new SLException("A step of the process has failed. Retrying it may solve the issue.");
            }
            getStepHelper().failStepIfProcessIsAborted(context);
        } catch (Exception e) {
            stepPhase = StepPhase.RETRY;
            handleException(context, e);
        } finally {
            context.setVariable(Variables.STEP_PHASE, stepPhase);
            postExecuteStep(context, stepPhase);
        }
    }

    protected StepPhase getInitialStepPhase(ProcessContext context) {
        return StepPhase.EXECUTE;
    }

    protected ProcessContext createProcessContext(DelegateExecution execution) {
        return new ProcessContext(execution, stepLogger, clientProvider);
    }

    private void handleException(ProcessContext context, Exception e) {
        try {
            StepPhase stepPhase = context.getVariable(Variables.STEP_PHASE);
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
     * Handle exception thrown during {@link #executeStep(ProcessContext) executeStep}
     * </p>
     * <p>
     * Can be overridden if standard behavior does not fulfill custom step requirements. For example, exception can be parsed to other
     * exception or not thrown at all.
     * </p>
     * <p>
     * 
     * @param context flowable context of the step
     * @param e thrown exception from {@link #executeStep(ProcessContext) executeStep} and pre-processed by
     *        {@link #handleException(DelegateExecution, Exception) handleException}
     */
    protected void onStepError(ProcessContext context, Exception e) throws Exception {
        processException(e, getStepErrorMessage(context), getErrorMessageAdditionalDescription(e, context));
    }

    protected void processException(Exception e, String detailedMessage, String description) throws Exception {
        e = handleControllerException(e);
        throw getExceptionConstructor(e).apply(e, detailedMessage + ": " + e.getMessage() + " " + description);
    }

    protected String getErrorMessageAdditionalDescription(Exception e, ProcessContext context) {
        if (e instanceof ContentException) {
            return StringUtils.EMPTY;
        }
        if (e instanceof CloudServiceBrokerException) {
            return getStepErrorMessageAdditionalDescription(context);
        }
        if (e instanceof CloudOperationException || e instanceof CloudControllerException) {
            return ExceptionMessageTailMapper.map(configuration, CloudComponents.CLOUD_CONTROLLER, null);
        }
        return ExceptionMessageTailMapper.map(configuration, CloudComponents.DEPLOY_SERVICE, null);
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

    protected void postExecuteStep(ProcessContext context, StepPhase stepState) {
        try {
            getStepHelper().postExecuteStep(context, stepState);
        } catch (SLException e) {
            getStepHelper().logExceptionAndStoreProgressMessage(context, e);
            throw e;
        }
    }

    protected abstract String getStepErrorMessage(ProcessContext context);
    
    protected abstract StepPhase executeStep(ProcessContext context) throws Exception;

    protected StepLogger getStepLogger() {
        if (stepLogger == null) {
            throw new IllegalStateException(Messages.STEP_LOGGER_NOT_INITIALIZED);
        }
        return stepLogger;
    }

    protected void initializeStepLogger(DelegateExecution execution) {
        stepLogger = stepLoggerFactory.create(execution, progressMessageService, processLoggerProvider, logger);
    }

    protected Exception getWithProperMessage(Exception e) {
        if (StringUtils.isEmpty(e.getMessage())) {
            return new Exception("An unknown error occurred", e);
        }
        return e;
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

    protected String getStepErrorMessageAdditionalDescription(ProcessContext context) {
        return StringUtils.EMPTY;
    }

}

package com.sap.cloud.lm.sl.cf.process.steps;

import static com.sap.activiti.common.Constants.CHARSET_UTF_8;
import static com.sap.activiti.common.Constants.LOGICAL_STEP_RETRY_MSG_SUFFIX;
import static com.sap.activiti.common.Constants.STEP_NAME_PREFIX;

import java.io.UnsupportedEncodingException;

import javax.inject.Inject;
import javax.inject.Named;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.slf4j.MDC;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.activiti.common.Logger;
import com.sap.activiti.common.LogicalRetryException;
import com.sap.activiti.common.api.ILogicalStep;
import com.sap.activiti.common.api.IStatusSignaller;
import com.sap.activiti.common.impl.StatusSignaller;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
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

public abstract class AbstractProcessStep implements StepIndexProvider, JavaDelegate, ILogicalStep {

    protected final Logger LOGGER = Logger.getInstance(getClass());

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

    private IStatusSignaller signaller;

    @Override
    public void execute(DelegateExecution context) throws Exception {
        failTaskIfRetryIsNeeded(context);
        ExecutionStatus status = null;
        getStatusSignaller();
        createStepLogger(context);
        try {
            MDC.put(Constants.ATTR_CORRELATION_ID, StepsUtil.getCorrelationId(context));
            getStepHelper().preExecuteStep(context, ExecutionStatus.NEW);
            status = executeStepInternal(context);
            clearRetryMessage(status, context);
            getStepHelper().failStepIfProcessIsAborted(context);
            LOGGER.debug(context, "Execution finished");
        } catch (MonitoringException e) {
            getStepLogger().errorWithoutProgressMessage(e.getMessage());
            status = ExecutionStatus.FAILED;
            handleException(context, e);
        } catch (Throwable t) {
            status = ExecutionStatus.FAILED;
            handleException(context, t);
        } finally {
            postExecuteStep(context, status);
            getSignaller().signal(context, status);
        }
    }

    private void handleException(DelegateExecution context, Throwable t) throws Exception {
        t = getWithProperMessage(t);
        getStepHelper().logException(context, t);
        throw t instanceof Exception ? (Exception) t : new Exception(t);
    }

    protected void postExecuteStep(DelegateExecution context, ExecutionStatus status) {
        try {
            getStepHelper().postExecuteStep(context, status);
        } catch (SLException e) {
            getStepHelper().storeExceptionInProgressMessageService(context, e);
            logException(context, e);
            throw e;
        }
    }

    protected abstract ExecutionStatus executeStepInternal(DelegateExecution context) throws Exception;

    private void clearRetryMessage(ExecutionStatus status, DelegateExecution context) {
        if (!ExecutionStatus.LOGICAL_RETRY.equals(status)) {
            context.removeVariable(getLogicalStepName() + LOGICAL_STEP_RETRY_MSG_SUFFIX);
        }
    }

    protected StepLogger getStepLogger() {
        if (stepLogger == null) {
            throw new IllegalStateException(Messages.STEP_LOGGER_NOT_INITIALIZED);
        }
        return stepLogger;
    }

    protected void createStepLogger(DelegateExecution context) {
        stepLogger = stepLoggerFactory.create(context, progressMessageService, processLoggerProviderFactory, LOGGER.getLoggerImpl());
    }

    protected CloudFoundryOperations getCloudFoundryClient(DelegateExecution context) throws SLException {
        return StepsUtil.getCloudFoundryClient(context, clientProvider, getStepLogger());
    }

    protected CloudFoundryOperations getCloudFoundryClient(DelegateExecution context, String org, String space) throws SLException {
        return StepsUtil.getCloudFoundryClient(context, clientProvider, getStepLogger(), org, space);
    }

    protected ClientExtensions getClientExtensions(DelegateExecution context) throws SLException {
        return StepsUtil.getClientExtensions(context, clientProvider, getStepLogger());
    }

    protected ClientExtensions getClientExtensions(DelegateExecution context, String org, String space) throws SLException {
        return StepsUtil.getClientExtensions(context, clientProvider, getStepLogger(), org, space);
    }

    protected Throwable getWithProperMessage(Throwable t) {
        if (t.getMessage() == null || t.getMessage().isEmpty()) {
            return new Exception("An unknown error occurred", t);
        }
        return t;
    }

    public void logException(DelegateExecution context, Throwable t) {
        getStepHelper().logException(context, t);
    }

    @Override
    public String getLogicalStepName() {
        return this.getClass().getSimpleName();
    }

    protected ProcessStepHelper getStepHelper() {
        if (stepHelper == null) {
            stepHelper = new ProcessStepHelper(getProgressMessageService(), getProcessLoggerProvider(), this, contextExtensionDao);
        }
        return stepHelper;
    }

    protected void setRetryMessage(DelegateExecution context, String retryMessage) {
        try {
            context.setVariable(getRetryMessageVariable(), retryMessage.getBytes(CHARSET_UTF_8));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    String getRetryMessageVariable() {
        return getLogicalStepName() + LOGICAL_STEP_RETRY_MSG_SUFFIX;
    }

    protected ProcessLoggerProviderFactory getProcessLoggerProvider() {
        return processLoggerProviderFactory;
    }

    protected ProgressMessageService getProgressMessageService() {
        return progressMessageService;
    }

    @Override
    public Integer getStepIndex(DelegateExecution context) {
        return getIndexVariable() != null ? (int) context.getVariable(getIndexVariable()) : -1;
    }

    protected String getIndexVariable() {
        return null;
    }

    protected String getStatusVariable() {
        return STEP_NAME_PREFIX + getLogicalStepName();
    }

    public IStatusSignaller getSignaller() {
        return this.signaller;
    }

    private void getStatusSignaller() {
        if (getSignaller() == null) {
            this.signaller = new StatusSignaller(getLogicalStepName());
        }
    }

    private void failTaskIfRetryIsNeeded(DelegateExecution context) {
        if (!isInLogicalRetry(context)) {
            return;
        }
        LOGGER.debug(context, "Task must be retried");
        String retryMessage = getRetryMessage(context);
        throw new LogicalRetryException(retryMessage);
    }

    private boolean isInLogicalRetry(DelegateExecution context) {
        String status = (String) context.getVariable(getStatusVariable());
        return ExecutionStatus.LOGICAL_RETRY.name().equalsIgnoreCase(status);
    }

    private String getRetryMessage(DelegateExecution context) {
        byte[] retryMsg = (byte[]) context.getVariable(getRetryMessageVariable());
        if (retryMsg == null || retryMsg.length == 0) {
            return Messages.NO_RETRY_MESSAGE;
        }
        try {
            return new String(retryMsg, CHARSET_UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}

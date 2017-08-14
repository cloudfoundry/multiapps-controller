package com.sap.cloud.lm.sl.cf.process.steps;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.apache.log4j.Logger;
import org.cloudfoundry.client.lib.CloudFoundryOperations;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.process.exception.MonitoringException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.services.TaskExtensionService;
import com.sap.cloud.lm.sl.slp.steps.AbstractSLProcessStepWithBridge;
import com.sap.cloud.lm.sl.slp.steps.SLProcessStepHelper;

public abstract class AbstractXS2ProcessStepWithBridge extends AbstractSLProcessStepWithBridge {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(AbstractXS2ProcessStepWithBridge.class);

    @Inject
    protected CloudFoundryClientProvider clientProvider;
    @Inject
    protected TaskExtensionService taskExtensionService;

    @Override
    protected ExecutionStatus pollStatus(DelegateExecution context) throws Exception {
        try {
            return pollStatusInternal(context);

        } catch (MonitoringException e) {
            StepsUtil.error(context, e.getMessage(), LOGGER, processLoggerProviderFactory);
            throw e;
        }

    }

    protected abstract ExecutionStatus pollStatusInternal(DelegateExecution context) throws Exception;

    @Override
    protected Logger getLogger(DelegateExecution context) {
        return StepsUtil.getLogger(context, processLoggerProviderFactory);
    }

    protected CloudFoundryOperations getCloudFoundryClient(DelegateExecution context, org.slf4j.Logger appLogger) throws SLException {
        return StepsUtil.getCloudFoundryClient(context, clientProvider, appLogger, processLoggerProviderFactory);
    }

    protected ClientExtensions getClientExtensions(DelegateExecution context, org.slf4j.Logger appLogger) throws SLException {
        return StepsUtil.getClientExtensions(context, clientProvider, appLogger, processLoggerProviderFactory);
    }

    protected void logActivitiTask(DelegateExecution context, org.slf4j.Logger appLogger) {
        StepsUtil.logActivitiTask(context, appLogger, progressMessageService, processLoggerProviderFactory);
    }

    protected void error(DelegateExecution context, String message, Exception e, org.slf4j.Logger appLogger) {
        StepsUtil.error(context, message, e, appLogger, progressMessageService, processLoggerProviderFactory);
    }

    protected void error(DelegateExecution context, String message, org.slf4j.Logger appLogger) {
        StepsUtil.error(context, message, appLogger, progressMessageService, processLoggerProviderFactory);
    }

    protected void warn(DelegateExecution context, String message, org.slf4j.Logger appLogger) {
        StepsUtil.warn(context, message, appLogger, progressMessageService, processLoggerProviderFactory);
    }

    protected void warn(DelegateExecution context, String message, Exception e, org.slf4j.Logger appLogger) {
        StepsUtil.warn(context, message, e, appLogger, progressMessageService, processLoggerProviderFactory);
    }

    protected void info(DelegateExecution context, String message, org.slf4j.Logger appLogger) {
        StepsUtil.info(context, message, appLogger, progressMessageService, processLoggerProviderFactory);
    }

    protected void debug(DelegateExecution context, String message, org.slf4j.Logger appLogger) {
        StepsUtil.debug(context, message, appLogger, processLoggerProviderFactory);
    }

    protected void trace(DelegateExecution context, String message, org.slf4j.Logger appLogger) {
        StepsUtil.trace(context, message, appLogger, processLoggerProviderFactory);
    }

    @Override
    protected SLProcessStepHelper getStepHelper() {
        if (stepHelper == null) {
            stepHelper = new XS2ProcessStepHelper(getProgressMessageService(), getProcessLoggerProvider(), this);
        }
        return stepHelper;
    }

}

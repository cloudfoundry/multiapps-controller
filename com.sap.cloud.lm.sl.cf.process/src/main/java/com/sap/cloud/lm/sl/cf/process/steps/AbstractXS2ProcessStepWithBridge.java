package com.sap.cloud.lm.sl.cf.process.steps;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.apache.log4j.Logger;
import org.cloudfoundry.client.lib.CloudFoundryOperations;

import com.sap.activiti.common.ExecutionStatus;
import com.sap.cloud.lm.sl.cf.client.ClientExtensions;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.slp.services.TaskExtensionService;
import com.sap.cloud.lm.sl.slp.steps.AbstractSLProcessStepWithBridge;

public abstract class AbstractXS2ProcessStepWithBridge extends AbstractSLProcessStepWithBridge {

    @Inject
    protected CloudFoundryClientProvider clientProvider;
    @Inject
    protected TaskExtensionService taskExtensionService;

    @Override
    protected ExecutionStatus pollStatus(DelegateExecution context) throws Exception {
        try {
            return pollStatusInternal(context);
        } catch (Exception e) {
            StepsUtil.setCtsExtensions(context, e, taskExtensionService);
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
        StepsUtil.logActivitiTask(context, appLogger, processLoggerProviderFactory);
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

    protected void info(DelegateExecution context, String message, org.slf4j.Logger appLogger) {
        StepsUtil.info(context, message, appLogger, progressMessageService, processLoggerProviderFactory);
    }

    protected void debug(DelegateExecution context, String message, org.slf4j.Logger appLogger) {
        StepsUtil.debug(context, message, appLogger, processLoggerProviderFactory);
    }

    protected void trace(DelegateExecution context, String message, org.slf4j.Logger appLogger) {
        StepsUtil.trace(context, message, appLogger, processLoggerProviderFactory);
    }

}

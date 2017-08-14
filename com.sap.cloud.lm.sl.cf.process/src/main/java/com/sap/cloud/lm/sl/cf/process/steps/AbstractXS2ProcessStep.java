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
import com.sap.cloud.lm.sl.slp.steps.AbstractSLProcessStep;
import com.sap.cloud.lm.sl.slp.steps.SLProcessStepHelper;

public abstract class AbstractXS2ProcessStep extends AbstractSLProcessStep {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(AbstractXS2ProcessStep.class);

    @Inject
    protected CloudFoundryClientProvider clientProvider;
    @Inject
    protected TaskExtensionService taskExtensionService;

    @Override
    protected Logger getLogger(DelegateExecution context) {
        return StepsUtil.getLogger(context, processLoggerProviderFactory);
    }

    @Override
    protected ExecutionStatus executeStep(DelegateExecution context) throws Exception {
        try {
            return executeStepInternal(context);
        } catch (MonitoringException e) {
            StepsUtil.setCtsExtensions(context, e, taskExtensionService);
            StepsUtil.error(context, e.getMessage(), LOGGER, processLoggerProviderFactory);
            throw e;
        } catch (Exception e) {
            StepsUtil.setCtsExtensions(context, e, taskExtensionService);
            throw e;
        }
    }

    protected abstract ExecutionStatus executeStepInternal(DelegateExecution context) throws Exception;

    protected CloudFoundryOperations getCloudFoundryClient(DelegateExecution context, org.slf4j.Logger appLogger) throws SLException {
        return StepsUtil.getCloudFoundryClient(context, clientProvider, appLogger, processLoggerProviderFactory);
    }

    protected CloudFoundryOperations getCloudFoundryClient(DelegateExecution context, org.slf4j.Logger appLogger, String org, String space)
        throws SLException {
        return StepsUtil.getCloudFoundryClient(context, clientProvider, appLogger, processLoggerProviderFactory, org, space);
    }

    protected ClientExtensions getClientExtensions(DelegateExecution context, org.slf4j.Logger appLogger) throws SLException {
        return StepsUtil.getClientExtensions(context, clientProvider, appLogger, processLoggerProviderFactory);
    }

    protected ClientExtensions getClientExtensions(DelegateExecution context, org.slf4j.Logger appLogger, String org, String space)
        throws SLException {
        return StepsUtil.getClientExtensions(context, clientProvider, appLogger, processLoggerProviderFactory, org, space);
    }

    protected void logActivitiTask(DelegateExecution context, org.slf4j.Logger logger) {
        StepsUtil.logActivitiTask(context, logger, progressMessageService, processLoggerProviderFactory);
    }

    protected void error(DelegateExecution context, String message, Exception e, org.slf4j.Logger appLogger) {
        StepsUtil.error(context, message, e, appLogger, progressMessageService, processLoggerProviderFactory);
    }

    protected void error(DelegateExecution context, String message, org.slf4j.Logger appLogger) {
        StepsUtil.error(context, message, appLogger, progressMessageService, processLoggerProviderFactory);
    }

    protected void warn(DelegateExecution context, String message, Exception e, org.slf4j.Logger appLogger) {
        StepsUtil.warn(context, message, e, appLogger, progressMessageService, processLoggerProviderFactory);
    }

    protected void warn(DelegateExecution context, String message, org.slf4j.Logger appLogger) {
        StepsUtil.warn(context, message, appLogger, progressMessageService, processLoggerProviderFactory);
    }

    protected void info(DelegateExecution context, String message, org.slf4j.Logger appLogger) {
        StepsUtil.info(context, message, appLogger, progressMessageService, processLoggerProviderFactory);
    }

    protected void debug(DelegateExecution context, String message, org.slf4j.Logger logger) {
        StepsUtil.debug(context, message, logger, processLoggerProviderFactory);
    }

    protected void trace(DelegateExecution context, String message, org.slf4j.Logger logger) {
        StepsUtil.trace(context, message, logger, processLoggerProviderFactory);
    }

    @Override
    protected SLProcessStepHelper getStepHelper() {
        if (stepHelper == null) {
            stepHelper = new XS2ProcessStepHelper(getProgressMessageService(), getProcessLoggerProvider(), this);
        }
        return stepHelper;
    }

}

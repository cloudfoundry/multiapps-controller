package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.process.steps.StepsUtil;
import com.sap.cloud.lm.sl.persistence.services.ProgressMessageService;
import com.sap.cloud.lm.sl.slp.listener.AbstractSLProcessExecutionListener;

public abstract class AbstractXS2ProcessExecutionListener extends AbstractSLProcessExecutionListener {

    private static final long serialVersionUID = 2L;

    @Inject
    private ProgressMessageService progressMessageService;

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

}

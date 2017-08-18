package com.sap.cloud.lm.sl.cf.process.listeners;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.persistence.services.ProgressMessageService;
import com.sap.cloud.lm.sl.slp.listener.AbstractSLProcessExecutionListener;

public abstract class AbstractXS2ProcessExecutionListener extends AbstractSLProcessExecutionListener {

    private static final long serialVersionUID = 2L;

    @Inject
    private ProgressMessageService progressMessageService;
    @Inject
    private StepLogger.Factory stepLoggerFactory;

    private StepLogger stepLogger;

    @Override
    public void notify(DelegateExecution context) throws Exception {
        try {
            this.stepLogger = createStepLogger(context);
            notifyInternal(context);
        } finally {
            writeLogs(context);
        }
    }

    protected abstract org.slf4j.Logger getLogger();

    protected StepLogger getStepLogger() {
        if (stepLogger == null) {
            throw new IllegalStateException(Messages.STEP_LOGGER_NOT_INITIALIZED);
        }
        return stepLogger;
    }

    private StepLogger createStepLogger(DelegateExecution context) {
        return stepLoggerFactory.create(context, progressMessageService, processLoggerProviderFactory, getLogger());
    }

}

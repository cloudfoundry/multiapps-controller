package com.sap.cloud.lm.sl.cf.process.listeners;

import java.io.IOException;
import java.text.MessageFormat;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.persistence.message.Constants;
import com.sap.cloud.lm.sl.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.persistence.services.ProcessLoggerProviderFactory;
import com.sap.cloud.lm.sl.persistence.services.ProgressMessageService;

public abstract class AbstractProcessExecutionListener implements ExecutionListener {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProcessExecutionListener.class);

    @Inject
    private ProgressMessageService progressMessageService;
    @Inject
    private StepLogger.Factory stepLoggerFactory;
    @Inject
    protected ProcessLoggerProviderFactory processLoggerProviderFactory;

    private StepLogger stepLogger;

    @Override
    public void notify(DelegateExecution context) throws Exception {
        try {
            this.stepLogger = createStepLogger(context);
            notifyInternal(context);
        } catch (Exception e) {
            logException(context, e, Messages.EXECUTION_OF_PROCESS_LISTENER_HAS_FAILED);
            throw new SLException(e, Messages.EXECUTION_OF_PROCESS_LISTENER_HAS_FAILED);
        } finally {
            finalizeLogs(context);
        }
    }

    protected void logException(DelegateExecution context, Throwable t, String message) {
        LOGGER.error(message, t);
        getProcessLogger(context).error(message, t);
    }

    protected org.apache.log4j.Logger getProcessLogger(DelegateExecution context) {
        return getProcessLoggerProviderFactory().getDefaultLoggerProvider()
            .getLogger(getCorrelationId(context), this.getClass()
                .getName());
    }

    private String getCorrelationId(DelegateExecution context) {
        return (String) context.getVariable(Constants.CORRELATION_ID);
    }

    protected void finalizeLogs(DelegateExecution context) {
        // Make sure that if the same thread is reused for a job of another process, it will get a
        // new logger:
        getProcessLoggerProviderFactory().removeAll();
        // Write the log messages:
        try {
            writeLogs(context);
        } catch (IOException | FileStorageException e) {
            LOGGER.warn(MessageFormat.format(Messages.COULD_NOT_PERSIST_LOGS_FILE, e.getMessage()), e);
        }
    }

    protected void writeLogs(DelegateExecution context) throws IOException, FileStorageException {
        getProcessLoggerProviderFactory().flush(context, getLogDir());
    }

    protected static String getLogDir() {
        return ProcessLoggerProviderFactory.LOG_DIR;
    }

    protected StepLogger getStepLogger() {
        if (stepLogger == null) {
            throw new IllegalStateException(Messages.STEP_LOGGER_NOT_INITIALIZED);
        }
        return stepLogger;
    }

    protected ProcessLoggerProviderFactory getProcessLoggerProviderFactory() {
        if (processLoggerProviderFactory == null) {
            processLoggerProviderFactory = ProcessLoggerProviderFactory.getInstance();
        }
        return processLoggerProviderFactory;
    }

    private StepLogger createStepLogger(DelegateExecution context) {
        return stepLoggerFactory.create(context, progressMessageService, processLoggerProviderFactory, getLogger());
    }

    protected abstract void notifyInternal(DelegateExecution context) throws Exception;

    protected abstract org.slf4j.Logger getLogger();

}

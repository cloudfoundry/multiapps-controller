package com.sap.cloud.lm.sl.cf.process.listeners;

import java.io.IOException;
import java.text.MessageFormat;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.ExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.persistence.services.ProcessLoggerProviderFactory;
import com.sap.cloud.lm.sl.cf.persistence.services.ProgressMessageService;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.StepLogger;

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
        } finally {
            finalizeLogs(context);
        }
    }

    protected void finalizeLogs(DelegateExecution context) {
        // Make sure that if the same thread is reused for a job of another process, it will get a
        // new logger:
        getProcessLoggerProviderFactory().removeAll();
        // Write the log messages:
        try {
            writeLogs(context);
        } catch (IOException | FileStorageException e) {
            logException(e);
        }
    }

    protected void writeLogs(DelegateExecution context) throws IOException, FileStorageException {
        getProcessLoggerProviderFactory().flushDefaultDir(context);
    }


    private static void logException(Exception e) {
        LOGGER.warn(MessageFormat.format(Messages.COULD_NOT_PERSIST_LOGS_FILE, e.getMessage()), e);
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

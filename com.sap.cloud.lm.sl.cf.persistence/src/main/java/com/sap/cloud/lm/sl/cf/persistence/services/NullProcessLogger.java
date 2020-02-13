package com.sap.cloud.lm.sl.cf.persistence.services;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.persistence.Messages;

public class NullProcessLogger extends ProcessLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(NullProcessLogger.class);

    public NullProcessLogger(String spaceId, String processId, String activityId) {
        super(spaceId, processId, activityId);
    }

    @Override
    public void info(Object message) {
        logNullCorrelationId();
    }

    @Override
    public void debug(Object message) {
        logNullCorrelationId();
    }

    @Override
    public void error(Object message) {
        logNullCorrelationId();
    }

    @Override
    public void error(Object message, Throwable t) {
        logNullCorrelationId();
    }

    @Override
    public void trace(Object message) {
        logNullCorrelationId();
    }

    @Override
    public void warn(Object message) {
        logNullCorrelationId();
    }

    @Override
    public void warn(Object message, Throwable t) {
        logNullCorrelationId();
    }

    @Override
    public synchronized void removeAllAppenders() {
        logNullCorrelationId();
    }

    @Override
    public synchronized void persistLogFile(ProcessLogsPersistenceService processLogsPersistenceService) {
        logNullCorrelationId();
    }

    @Override
    public synchronized void deleteLogFile() {
        logNullCorrelationId();
    }

    public void logNullCorrelationId() {
        LOGGER.info(MessageFormat.format(Messages.ERROR_CORRELATION_ID_OR_ACTIVITY_ID_NULL, processId, activityId, spaceId));
    }
}

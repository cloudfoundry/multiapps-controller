package org.cloudfoundry.multiapps.controller.persistence.services;

import org.apache.logging.log4j.core.LoggerContext;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

public class NullProcessLogger extends ProcessLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(NullProcessLogger.class);
    private static final String NULL_LOGGER_NAME = "NULL_LOGGER";

    public NullProcessLogger(String spaceId, String processId, String activityId) {
        super(LoggerContext.getContext(false), spaceId, processId, activityId);
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
    public synchronized void persistLogFile(ProcessLogsPersistenceService processLogsPersistenceService) {
        logNullCorrelationId();
    }

    @Override
    public String getLoggerName() {
        return NULL_LOGGER_NAME;
    }

    @Override
    public synchronized void deleteLogFile() {
        logNullCorrelationId();
    }

    public void logNullCorrelationId() {
        LOGGER.info(MessageFormat.format(Messages.ERROR_CORRELATION_ID_OR_ACTIVITY_ID_NULL, processId, activityId, spaceId));
    }
}

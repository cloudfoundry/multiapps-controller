package org.cloudfoundry.multiapps.controller.persistence.services;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.util.Strings;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableOperationLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

public class NullProcessLogger extends ProcessLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(NullProcessLogger.class);
    private static final String NULL_LOGGER_NAME = "NULL_LOGGER";
    private final String spaceId;
    private final String processId;
    private final String activityId;

    public NullProcessLogger(String spaceId, String processId, String activityId) {
        super(null, NULL_LOGGER_NAME, null, Strings.EMPTY);
        this.activityId = activityId;
        this.spaceId = spaceId;
        this.processId = processId;
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

    public void logNullCorrelationId() {
        LOGGER.info(MessageFormat.format(Messages.ERROR_CORRELATION_ID_OR_ACTIVITY_ID_NULL, processId, activityId, spaceId));
    }
}

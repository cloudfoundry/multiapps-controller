package org.cloudfoundry.multiapps.controller.persistence.services;

import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.cloudfoundry.multiapps.controller.persistence.Messages;

public class ProcessLogger {

    protected final String spaceId;
    protected final String processId;
    protected final String activityId;
    private Logger logger;
    private LoggerContext loggerContext;
    private ProcessLoggerProvider.LogDbAppender logDbAppender;

    public ProcessLogger(LoggerContext loggerContext, Logger logger, String spaceId, String processId, String activityId,
                         ProcessLoggerProvider.LogDbAppender logDbAppender) {
        this.logger = logger;
        this.spaceId = spaceId;
        this.processId = processId;
        this.activityId = activityId;
        this.loggerContext = loggerContext;
        this.logDbAppender = logDbAppender;
    }

    protected ProcessLogger(LoggerContext loggerContext, String spaceId, String processId, String activityId, ProcessLoggerProvider.LogDbAppender logDbAppender) {
        this.loggerContext = loggerContext;
        this.logger = loggerContext.getRootLogger();
        this.spaceId = spaceId;
        this.processId = processId;
        this.activityId = activityId;
        this.logDbAppender = logDbAppender;
    }

    public void info(Object message) {
        logger.info(message);
    }

    public void debug(Object message) {
        logger.debug(message);
    }

    public void debug(Object message, Throwable throwable) {
        logger.debug(message, throwable);
    }

    public void error(Object message) {
        logger.error(message);
    }

    public void error(Object message, Throwable t) {
        logger.error(message, t);
    }

    public void trace(Object message) {
        logger.trace(message);
    }

    public void warn(Object message) {
        logger.warn(message);
    }

    public void warn(Object message, Throwable t) {
        logger.warn(message, t);
    }

    public String getProcessId() {
        return this.processId;
    }

    public String getActivityId() {
        return this.activityId;
    }

    public String getLoggerName() {
        return this.logger.getName();
    }

    public void closeLoggerContext() {
        try {
            //logDbAppender.stop();
            loggerContext.getRootLogger()
                         .removeAppender(logDbAppender);
            loggerContext.getConfiguration()
                         .getAppenders()
                         .remove(logDbAppender.getName());
            loggerContext.updateLoggers();
        } catch (Exception exception) {
            logger.error(Messages.COULD_NOT_CLOSE_LOGGER_CONTEXT, exception);
        }
    }
}

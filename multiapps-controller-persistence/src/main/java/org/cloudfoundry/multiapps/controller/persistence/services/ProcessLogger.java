package org.cloudfoundry.multiapps.controller.persistence.services;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.cloudfoundry.multiapps.controller.persistence.Messages;

import java.io.File;

public class ProcessLogger {

    private Logger logger;
    private File log;
    private String logName;
    protected final String spaceId;
    protected final String processId;
    protected final String activityId;
    private LoggerContext loggerContext;

    public ProcessLogger(LoggerContext loggerContext, Logger logger, File log, String logName, String spaceId, String processId,
                         String activityId) {
        this.logger = logger;
        this.log = log;
        this.logName = logName;
        this.spaceId = spaceId;
        this.processId = processId;
        this.activityId = activityId;
        this.loggerContext = loggerContext;
    }

    protected ProcessLogger(LoggerContext loggerContext, String spaceId, String processId, String activityId) {
        this.loggerContext = loggerContext;
        this.logger = loggerContext.getRootLogger();
        this.spaceId = spaceId;
        this.processId = processId;
        this.activityId = activityId;
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

    public synchronized void persistLogFile(ProcessLogsPersistenceService processLogsPersistenceService) {
        if (log.exists()) {
            processLogsPersistenceService.persistLog(spaceId, processId, log, logName);
        }
    }

    public synchronized void deleteLogFile() {
        FileUtils.deleteQuietly(log);
    }

    public String getLoggerName() {
        return this.logger.getName();
    }

    public void closeLoggerContext() {
        try {
            loggerContext.close();
        } catch (Exception exception) {
            logger.error(Messages.COULD_NOT_CLOSE_LOGGER_CONTEXT, exception);
        }
    }
}

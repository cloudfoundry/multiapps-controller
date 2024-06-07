package org.cloudfoundry.multiapps.controller.persistence.services;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.util.WatchManager;
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
    private FileAppender fileAppender;

    public ProcessLogger(LoggerContext loggerContext, Logger logger, File log, String logName, String spaceId, String processId,
                         String activityId, FileAppender fileAppender) {
        this.logger = logger;
        this.log = log;
        this.logName = logName;
        this.spaceId = spaceId;
        this.processId = processId;
        this.activityId = activityId;
        this.loggerContext = loggerContext;
        this.fileAppender = fileAppender;
    }

    protected ProcessLogger(LoggerContext loggerContext, String spaceId, String processId, String activityId, FileAppender fileAppender) {
        this.loggerContext = loggerContext;
        this.logger = loggerContext.getRootLogger();
        this.spaceId = spaceId;
        this.processId = processId;
        this.activityId = activityId;
        this.fileAppender = fileAppender;
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
            loggerContext.getRootLogger().removeAppender(fileAppender);
            loggerContext.getConfiguration().getAppenders().remove(fileAppender.getName());
            fileAppender.stop();
            loggerContext.stop();
        } catch (Exception exception) {
            logger.error(Messages.COULD_NOT_CLOSE_LOGGER_CONTEXT, exception);
        }
    }
}

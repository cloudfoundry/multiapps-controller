package com.sap.cloud.lm.sl.cf.persistence.services;

import java.io.File;

import com.sap.cloud.lm.sl.cf.persistence.message.Messages;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;

public class ProcessLogger {

    private final Logger logger;
    private final LoggerContext loggerContext;
    private final File log;
    private final String logName;
    private final String spaceId;
    private final String processId;
    private final String activityId;

    public ProcessLogger(LoggerContext loggerContext, Logger logger, File log, String logName, String spaceId, String processId,
                         String activityId) {
        this.loggerContext = loggerContext;
        this.logger = logger;
        this.log = log;
        this.logName = logName;
        this.spaceId = spaceId;
        this.processId = processId;
        this.activityId = activityId;
    }

    public String getName() {
        return logger.getName();
    }

    public void info(Object message) {
        logger.info(message);
    }

    public void debug(Object message) {
        logger.debug(message);
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
        return processId;
    }

    public String getActivityId() {
        return activityId;
    }

    public synchronized void persistLogFile(ProcessLogsPersistenceService processLogsPersistenceService) {
        if (log.exists()) {
            processLogsPersistenceService.appendLog(spaceId, processId, log, logName);
        }
    }

    public synchronized void deleteLogFile() {
        FileUtils.deleteQuietly(log);
    }

    public void close() {
        try {
            loggerContext.close();
        } catch (Exception e) {
            logger.error(Messages.CANNOT_CLOSE_LOGGER_CONTEXT, e);
        }
    }
}

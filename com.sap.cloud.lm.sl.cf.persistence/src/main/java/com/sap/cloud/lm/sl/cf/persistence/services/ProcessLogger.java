package com.sap.cloud.lm.sl.cf.persistence.services;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

public class ProcessLogger extends Logger {

    private Logger logger;
    private File log;
    private String logName;
    private String spaceId;
    private String processId;
    private String activityId;

    public ProcessLogger(Logger logger, File log, String logName, String spaceId, String processId, String activityId) {
        super(logger.getName());
        this.logger = logger;
        this.log = log;
        this.logName = logName;
        this.spaceId = spaceId;
        this.processId = processId;
        this.activityId = activityId;
    }

    @Override
    public void info(Object message) {
        logger.info(message);
    }

    @Override
    public void debug(Object message) {
        logger.debug(message);
    }

    @Override
    public void error(Object message) {
        logger.error(message);
    }

    @Override
    public void error(Object message, Throwable t) {
        logger.error(message, t);
    }

    @Override
    public void trace(Object message) {
        logger.trace(message);
    }

    @Override
    public void warn(Object message) {
        logger.warn(message);
    }

    @Override
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

}

package com.sap.cloud.lm.sl.cf.persistence.services;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.persistence.Constants;

public class ProcessLoggerProvider {

    private static final String LOG_LAYOUT = "#2.0#%d{yyyy MM dd HH:mm:ss.SSS}#%d{XXX}#%p#%c#%n%X{MsgCode}#%X{CSNComponent}#%X{DCComponent}##%X{DSRCorrelationId}#%X{Application}#%C#%X{User}#%X{Session}#%X{Transaction}#%X{DSRRootContextId}#%X{DSRTransaction}#%X{DSRConnection}#%X{DSRCounter}#%t##%X{ResourceBundle}#%n%m#%n%n";
    private static final String PARENT_LOGGER = "com.sap.cloud.lm.sl.xs2";
    private static final String DEFAULT_LOG_NAME = "MAIN_LOG";
    private static final String DEFAULT_LOG_DIR = "logs";
    private static final String LOG_FILE_EXTENSION = ".log";

    private Map<String, ProcessLogger> loggersCache = new ConcurrentHashMap<>();

    public ProcessLogger getLogger(DelegateExecution context) {
        return getLogger(context, DEFAULT_LOG_NAME);
    }

    public ProcessLogger getLogger(DelegateExecution context, String logName) {
        return getLogger(context, logName, null);
    }

    public ProcessLogger getLogger(DelegateExecution context, String logName, PatternLayout layout) {
        String name = getLoggerName(context, logName);
        String correlationId = getCorrelationId(context);
        String spaceId = getSpaceId(context);
        String activityId = getTaskId(context);
        if (correlationId == null || activityId == null) {
            return new NullProcessLogger(spaceId, context.getProcessInstanceId(), activityId);
        }
        return loggersCache.computeIfAbsent(name, (String loggerName) -> createProcessLogger(spaceId, correlationId, activityId, loggerName,
                                                                                             logName, layout));
    }

    private String getLoggerName(DelegateExecution context, String logName) {
        return PARENT_LOGGER + '.' + getCorrelationId(context) + '.' + logName + '.' + getTaskId(context);
    }

    private String getCorrelationId(DelegateExecution context) {
        return (String) context.getVariable(Constants.CORRELATION_ID);
    }

    private String getTaskId(DelegateExecution context) {
        String taskId = (String) context.getVariable(Constants.TASK_ID);
        return taskId != null ? taskId : context.getCurrentActivityId();
    }

    private ProcessLogger createProcessLogger(String spaceId, String correlationId, String activityId, String loggerName, String logName,
                                              PatternLayout layout) {
        Logger logger = Logger.getLogger(loggerName);
        File logFile = getLocalFile(loggerName);
        logger.removeAllAppenders();
        logger.addAppender(createAppender(logger.getLevel(), logFile, layout));
        return new ProcessLogger(logger, logFile, logName, spaceId, correlationId, activityId);
    }

    protected File getLocalFile(String loggerName) {
        String fileName = loggerName + LOG_FILE_EXTENSION;
        return new File(DEFAULT_LOG_DIR, fileName);
    }

    private Appender createAppender(Level level, File logFile, PatternLayout layout) {
        FileAppender appender = new FileAppender();
        if (layout == null) {
            layout = new PatternLayout(LOG_LAYOUT);
        }
        appender.setLayout(layout);
        appender.setFile(logFile.getAbsolutePath());
        appender.setThreshold(level);
        appender.setAppend(true);
        appender.activateOptions();
        return appender;
    }

    private String getSpaceId(DelegateExecution context) {
        return (String) context.getVariable(Constants.VARIABLE_NAME_SPACE_ID);
    }

    public List<ProcessLogger> getExistingLoggers(String processId, String activityId) {
        return loggersCache.values()
                           .stream()
                           .filter(logger -> hasLoggerSpecificProcessIdAndActivityId(processId, activityId, logger))
                           .collect(Collectors.toList());
    }

    private boolean hasLoggerSpecificProcessIdAndActivityId(String processId, String activityId, ProcessLogger logger) {
        return processId.equals(logger.getProcessId()) && activityId.equals(logger.getActivityId());
    }

    public void remove(ProcessLogger processLogger) {
        processLogger.removeAllAppenders();
        loggersCache.remove(processLogger.getName());
    }

}

package com.sap.cloud.lm.sl.cf.persistence.services;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.persistence.Constants;

@Named("processLoggerProvider")
public class ProcessLoggerProvider {

    private static final String LOG_LAYOUT = "#2.0#%d{yyyy MM dd HH:mm:ss.SSS}#%d{XXX}#%p#%c#%n%X{MsgCode}#%X{CSNComponent}#%X{DCComponent}##%X{DSRCorrelationId}#%X{Application}#%C#%X{User}#%X{Session}#%X{Transaction}#%X{DSRRootContextId}#%X{DSRTransaction}#%X{DSRConnection}#%X{DSRCounter}#%t##%X{ResourceBundle}#%n%m#%n%n";
    private static final String PARENT_LOGGER = "com.sap.cloud.lm.sl.xs2";
    private static final String DEFAULT_LOG_NAME = "OPERATION";
    private static final String DEFAULT_LOG_DIR = "logs";
    private static final String LOG_FILE_EXTENSION = ".log";

    private final Map<String, ProcessLogger> loggersCache = new ConcurrentHashMap<>();

    public ProcessLogger getLogger(DelegateExecution execution) {
        return getLogger(execution, DEFAULT_LOG_NAME);
    }

    public ProcessLogger getLogger(DelegateExecution execution, String logName) {
        return getLogger(execution, logName, null);
    }

    public ProcessLogger getLogger(DelegateExecution execution, String logName, PatternLayout layout) {
        String name = getLoggerName(execution, logName);
        String correlationId = getCorrelationId(execution);
        String spaceId = getSpaceId(execution);
        String activityId = getTaskId(execution);
        String logNameWithExtension = logName + LOG_FILE_EXTENSION;
        if (correlationId == null || activityId == null) {
            return new NullProcessLogger(spaceId, execution.getProcessInstanceId(), activityId);
        }
        return loggersCache.computeIfAbsent(name, (String loggerName) -> createProcessLogger(spaceId, correlationId, activityId, loggerName,
                                                                                             logNameWithExtension, layout));
    }

    private String getLoggerName(DelegateExecution execution, String logName) {
        return PARENT_LOGGER + '.' + getCorrelationId(execution) + '.' + logName + '.' + getTaskId(execution);
    }

    private String getCorrelationId(DelegateExecution execution) {
        return (String) execution.getVariable(Constants.CORRELATION_ID);
    }

    private String getTaskId(DelegateExecution execution) {
        String taskId = (String) execution.getVariable(Constants.TASK_ID);
        return taskId != null ? taskId : execution.getCurrentActivityId();
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

    private String getSpaceId(DelegateExecution execution) {
        return (String) execution.getVariable(Constants.VARIABLE_NAME_SPACE_ID);
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

package org.cloudfoundry.multiapps.controller.persistence.services;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.flowable.engine.delegate.DelegateExecution;

import javax.inject.Named;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    public ProcessLogger getLogger(DelegateExecution execution, String logName, AbstractStringLayout layout) {
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
                                              AbstractStringLayout abstractStringLayout) {
        File logFile = getLocalFileByLoggerName(loggerName);
        LoggerContext loggerContext = initializeLoggerContext(loggerName, logFile, abstractStringLayout);
        Logger logger = loggerContext.getLogger(loggerName);
        return new ProcessLogger(loggerContext, logger, logFile, logName, spaceId, correlationId, activityId);
    }

    protected File getLocalFileByLoggerName(String loggerName) {
        String fileName = loggerName + LOG_FILE_EXTENSION;
        return new File(DEFAULT_LOG_DIR, fileName);
    }

    private LoggerContext initializeLoggerContext(String loggerName, File logFile, AbstractStringLayout abstractStringLayout) {

        LoggerContext loggerContext = new LoggerContext(loggerName);
        FileAppender fileAppender = initializeStartedAppender(loggerContext, logFile, abstractStringLayout, loggerName);
        fileAppender.start();
        loggerContext.getConfiguration()
                     .addAppender(fileAppender);
        LoggerConfig loggerConfig = getLoggerConfig(loggerContext, loggerName);
        setLoggerConfigLoggingLevel(loggerConfig, Level.DEBUG);
        addAppenderToLoggerConfig(loggerConfig, fileAppender, Level.DEBUG);
        addFileAppenderToRootLogger(loggerContext, fileAppender);
        disableConsoleLogging(loggerContext);
        loggerContext.updateLoggers();

        return loggerContext;
    }

    private FileAppender initializeStartedAppender(LoggerContext loggerContext, File logFile, AbstractStringLayout layout,
                                                   String loggerName) {
        if (layout == null) {
            layout = PatternLayout.newBuilder()
                                  .withPattern(LOG_LAYOUT)
                                  .build();
        }
        return FileAppender.newBuilder()
                           .setName(loggerName)
                           .withAppend(true)
                           .withFileName(logFile.toString())
                           .setLayout(layout)
                           .setConfiguration(loggerContext.getConfiguration())
                           .withLocking(true)
                           .build();
    }

    private LoggerConfig getLoggerConfig(LoggerContext loggerContext, String loggerName) {
        return loggerContext.getConfiguration()
                            .getLoggerConfig(loggerName);
    }

    private void setLoggerConfigLoggingLevel(LoggerConfig loggerConfig, Level level) {
        loggerConfig.setLevel(level != null ? level : Level.DEBUG);
    }

    private void addAppenderToLoggerConfig(LoggerConfig loggerConfig, FileAppender fileAppender, Level level) {
        loggerConfig.addAppender(fileAppender, level != null ? level : Level.DEBUG, null);
    }

    private void addFileAppenderToRootLogger(LoggerContext loggerContext, FileAppender fileAppender) {
        loggerContext.getRootLogger()
                     .addAppender(fileAppender);
    }

    private void disableConsoleLogging(LoggerContext loggerContext) {
        for (Appender appender : getAllAppenders(loggerContext)) {
            if (appender.getName()
                        .contains("DefaultConsole")) {
                loggerContext.getRootLogger()
                             .removeAppender(appender);
            }
        }
    }

    private Collection<Appender> getAllAppenders(LoggerContext loggerContext) {
        return Collections.unmodifiableCollection(loggerContext.getRootLogger()
                                                               .getAppenders()
                                                               .values());
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

    public void removeLoggersCache(ProcessLogger processLogger) {
        loggersCache.remove(processLogger.getLoggerName());
    }
}

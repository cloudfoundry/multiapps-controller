package com.sap.cloud.lm.sl.cf.persistence.services;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Named;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.persistence.message.Constants;
import com.sap.cloud.lm.sl.common.SLException;

@Named("processLoggerProvider")
public class ProcessLoggerProvider {

    private static final String LOG_LAYOUT = "#2.0#%d{yyyy MM dd HH:mm:ss.SSS}#%d{XXX}#%p#%c#%n%X{MsgCode}#%X{CSNComponent}#%X{DCComponent}##%X{DSRCorrelationId}#%X{Application}#%C#%X{User}#%X{Session}#%X{Transaction}#%X{DSRRootContextId}#%X{DSRTransaction}#%X{DSRConnection}#%X{DSRCounter}#%t##%X{ResourceBundle}#%n%m#%n%n";
    private static final String PARENT_LOGGER = "com.sap.cloud.lm.sl.xs2";
    private static final String DEFAULT_LOG_NAME = "MAIN_LOG";
    private static final String DEFAULT_LOG_DIR = "logs";
    private static final String LOG_FILE_EXTENSION = ".log";

    private final Map<String, ProcessLogger> loggersCache = new ConcurrentHashMap<>();

    public ProcessLogger getLogger(DelegateExecution context) {
        return getLogger(context, DEFAULT_LOG_NAME);
    }

    public ProcessLogger getLogger(DelegateExecution context, String logName) {
        return getLogger(context, logName, loggerContext -> PatternLayout.newBuilder()
                                                                         .withPattern(LOG_LAYOUT)
                                                                         .withConfiguration(loggerContext.getConfiguration())
                                                                         .build());
    }

    public ProcessLogger getLogger(DelegateExecution context, String logName,
                                   Function<LoggerContext, AbstractStringLayout> layoutCreatorFunction) {
        String name = getLoggerName(context, logName);
        return loggersCache.computeIfAbsent(name, loggerName -> createProcessLogger(context, loggerName, logName, layoutCreatorFunction));
    }

    private String getLoggerName(DelegateExecution context, String logName) {
        return new StringBuilder(PARENT_LOGGER).append('.')
                                               .append(getCorrelationId(context))
                                               .append('.')
                                               .append(logName)
                                               .append('.')
                                               .append(getTaskId(context))
                                               .toString();
    }

    private String getCorrelationId(DelegateExecution context) {
        return (String) context.getVariable(Constants.CORRELATION_ID);
    }

    private String getTaskId(DelegateExecution context) {
        String taskId = (String) context.getVariable(Constants.TASK_ID);
        return taskId != null ? taskId : context.getCurrentActivityId();
    }

    private ProcessLogger createProcessLogger(DelegateExecution context, String loggerName, String logName,
                                              Function<LoggerContext, AbstractStringLayout> layoutCreatorFunction) {
        File logFile = getLocalFile(loggerName);
        LoggerContext loggerContext = initLoggerContext(loggerName, logFile, layoutCreatorFunction);
        Logger logger = loggerContext.getLogger(loggerName);
        return new ProcessLogger(loggerContext,
                                 logger,
                                 logFile,
                                 logName,
                                 getSpaceId(context),
                                 getCorrelationId(context),
                                 getTaskId(context));
    }

    private LoggerContext initLoggerContext(String loggerName, File logFile,
                                            Function<LoggerContext, AbstractStringLayout> layoutCreatorFunction) {
        LoggerContext loggerContext = new LoggerContext(loggerName);
        try {
            attachFileAppender(loggerName, logFile, layoutCreatorFunction, loggerContext);
        } catch (Exception e) {
            loggerContext.close();
            throw new SLException(e, e.getMessage());
        }
        return loggerContext;
    }

    private void attachFileAppender(String loggerName, File logFile, Function<LoggerContext, AbstractStringLayout> layoutCreatorFunction,
                                    LoggerContext loggerContext) {
        FileAppender fileAppender = createFileAppender(loggerName, logFile, layoutCreatorFunction, loggerContext);
        fileAppender.start();
        loggerContext.getConfiguration()
                     .addAppender(fileAppender);
        LoggerConfig loggerConfig = loggerContext.getConfiguration()
                                                 .getLoggerConfig(loggerName);
        loggerConfig.setLevel(Level.DEBUG);
        loggerConfig.addAppender(fileAppender, Level.DEBUG, null);
        loggerContext.getRootLogger()
                     .addAppender(fileAppender);
        disableConsoleLogging(loggerContext);
        loggerContext.updateLoggers();
    }

    private File getLocalFile(String loggerName) {
        return new File(DEFAULT_LOG_DIR, loggerName + LOG_FILE_EXTENSION);
    }

    private FileAppender createFileAppender(String loggerName, File logFile,
                                            Function<LoggerContext, AbstractStringLayout> layoutCreatorFunction, LoggerContext context) {
        return FileAppender.newBuilder()
                           .setName(loggerName)
                           .withFileName(logFile.toString())
                           .setLayout(layoutCreatorFunction.apply(context))
                           .setConfiguration(context.getConfiguration())
                           .build();
    }

    private void disableConsoleLogging(LoggerContext loggerContext) {
        for (Appender appender : getAllAppenders(loggerContext)) {
            if (appender.getName()
                        .contains(Constants.DEFAULT_CONSOLE_LOGGER_NAME)) {
                loggerContext.getRootLogger()
                             .removeAppender(appender);
            }
        }
    }

    private Collection<Appender> getAllAppenders(LoggerContext loggerContext) {
        return new ArrayList<>(loggerContext.getRootLogger()
                                            .getAppenders()
                                            .values());
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
        return logger.getProcessId()
                     .equals(processId)
            && logger.getActivityId()
                     .equals(activityId);
    }

    public void remove(ProcessLogger processLogger) {
        loggersCache.remove(processLogger.getName());
    }

}

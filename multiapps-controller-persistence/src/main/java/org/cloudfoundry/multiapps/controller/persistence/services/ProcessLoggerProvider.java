package org.cloudfoundry.multiapps.controller.persistence.services;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.filter.LevelMatchFilter;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.cloudfoundry.multiapps.controller.persistence.Messages;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.query.providers.SqlOperationLogQueryProvider;
import org.flowable.engine.delegate.DelegateExecution;

@Named("processLoggerProvider")
public class ProcessLoggerProvider {

    static final String LOG_LAYOUT = "#2.0#%d{yyyy MM dd HH:mm:ss.SSS}#%d{XXX}#%p#%c#%n%X{MsgCode}#%X{CSNComponent}#%X{DCComponent}##%X{DSRCorrelationId}#%X{Application}#%C#%X{User}#%X{Session}#%X{Transaction}#%X{DSRRootContextId}#%X{DSRTransaction}#%X{DSRConnection}#%X{DSRCounter}#%t##%X{ResourceBundle}#%n%m#%n%n";
    private static final ThresholdFilter FILTER = ThresholdFilter.createFilter(Level.ALL, Filter.Result.ACCEPT, Filter.Result.ACCEPT);
    LevelMatchFilter DEBUG_FILTER = LevelMatchFilter.newBuilder().setLevel(Level.DEBUG).setOnMatch(Filter.Result.ACCEPT).setOnMismatch(Filter.Result.ACCEPT).build();
    LevelMatchFilter ERROR_FILTER = LevelMatchFilter.newBuilder().setLevel(Level.ERROR).setOnMatch(Filter.Result.ACCEPT).setOnMismatch(Filter.Result.ACCEPT).build();
    LevelMatchFilter INFO_FILTER = LevelMatchFilter.newBuilder().setLevel(Level.INFO).setOnMatch(Filter.Result.ACCEPT).setOnMismatch(Filter.Result.ACCEPT).build();
    LevelMatchFilter WARN_FILTER = LevelMatchFilter.newBuilder().setLevel(Level.WARN).setOnMatch(Filter.Result.ACCEPT).setOnMismatch(Filter.Result.ACCEPT).build();
    LevelMatchFilter ALL_FILTER = LevelMatchFilter.newBuilder().setLevel(Level.DEBUG).setOnMatch(Filter.Result.ACCEPT).setOnMismatch(Filter.Result.ACCEPT).build();
    private static final String PARENT_LOGGER = "com.sap.cloud.lm.sl.xs2";
    private static final String DEFAULT_LOG_NAME = "OPERATION";
    private static final String DEFAULT_LOG_DIR = "logs";
    private static final String LOG_FILE_EXTENSION = ".log";
    private final DataSource dataSource;
    private final SqlOperationLogQueryProvider sqlOperationLogQueryProvider;

    private final Map<String, ProcessLogger> loggersCache = new ConcurrentHashMap<>();
    private final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);

    @Inject
    public ProcessLoggerProvider(DataSource dataSource) {
        this.dataSource = dataSource;
        sqlOperationLogQueryProvider = new SqlOperationLogQueryProvider();
    }

    public ProcessLogger getLogger(DelegateExecution execution) {

        return getLogger(execution, DEFAULT_LOG_NAME);
    }

    public ProcessLogger getLogger(DelegateExecution context, String logName) {
        return getLogger(context, logName, loggerContextDel -> PatternLayout.newBuilder()
                                                                            .withPattern(LOG_LAYOUT)
                                                                            .withConfiguration(loggerContextDel.getConfiguration())
                                                                            .build());
    }

    public ProcessLogger getLogger(DelegateExecution execution, String logName,
                                   Function<LoggerContext, AbstractStringLayout> layoutCreatorFunction) {
        String name = getLoggerName(execution, logName);
        String correlationId = getCorrelationId(execution);
        String spaceId = getSpaceId(execution);
        String activityId = getTaskId(execution);
        String logNameWithExtension = logName + LOG_FILE_EXTENSION;
        if (correlationId == null || activityId == null) {
            return new NullProcessLogger(spaceId, execution.getProcessInstanceId(), activityId);
        }
        return loggersCache.computeIfAbsent(name, (String loggerName) -> createProcessLogger(spaceId, correlationId, activityId, loggerName,
                                                                                             logNameWithExtension, layoutCreatorFunction));
    }

    private OperationLogEntry createOperationLogEntry(String operationId, String space, String operationLogName) {
        return ImmutableOperationLogEntry.builder()
                                         .operationId(operationId)
                                         .space(space)
                                         .operationLogName(operationLogName)
                                         .build();
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

    private synchronized ProcessLogger createProcessLogger(String spaceId, String correlationId, String activityId, String loggerName,
                                                           String logName,
                                                           Function<LoggerContext, AbstractStringLayout> layoutCreatorFunction) {
        OperationLogEntry operationLogEntry = createOperationLogEntry(correlationId, spaceId, logName);

        LogDbAppender logDbAppender = createLogDbAppender(operationLogEntry, loggerName, layoutCreatorFunction.apply(loggerContext));
        attachFileAppender(loggerName, logDbAppender);

        Logger logger = loggerContext.getLogger(loggerName);
        return new ProcessLogger(loggerContext, logger, logName, spaceId, correlationId, activityId, logDbAppender);
    }

    private void attachFileAppender(String loggerName, LogDbAppender logDbAppender) {
        logDbAppender.start();
        loggerContext.getConfiguration()
                     .addAppender(logDbAppender);
        LoggerConfig loggerConfig = getLoggerConfig(loggerContext, loggerName);
        //logDbAppender.addFilter(A);loggerConfig.addFilter(A);
        setLoggerConfigLoggingLevel(loggerConfig, Level.DEBUG);
        addAppenderToLoggerConfig(loggerConfig, logDbAppender, Level.DEBUG);
        addFileAppenderToRootLogger(loggerContext, logDbAppender);
        disableConsoleLogging(loggerContext);
        loggerContext.updateLoggers();
    }

    private LogDbAppender createLogDbAppender(OperationLogEntry operationLogEntry, String name, Layout<? extends Serializable> layout) {
        return new LogDbAppender(operationLogEntry, name, layout);
    }

    private LoggerConfig getLoggerConfig(LoggerContext loggerContext, String loggerName) {
        return LoggerConfig.newBuilder()
                           .withConfig(loggerContext.getConfiguration())
                           .withLoggerName(loggerName)
                           .build();
    }

    private void setLoggerConfigLoggingLevel(LoggerConfig loggerConfig, Level level) {
        loggerConfig.setLevel(level != null ? level : Level.DEBUG);
    }

    private void addAppenderToLoggerConfig(LoggerConfig loggerConfig, LogDbAppender logDbAppender, Level level) {
        loggerConfig.addAppender(logDbAppender, level != null ? level : Level.DEBUG, ALL_FILTER);
    }

    private void addFileAppenderToRootLogger(LoggerContext loggerContext, LogDbAppender logDbAppender) {
        loggerContext.getRootLogger()
                     .addAppender(logDbAppender);
        loggerContext
                .addFilter(DEBUG_FILTER);

    }

    private void disableConsoleLogging(LoggerContext loggerContext) {
        for (Appender appender : getAllAppenders(loggerContext)) {
            if (appender.getName()
                        .contains(Messages.DEFAULT_CONSOLE)) {
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

    public class LogDbAppender extends AbstractAppender {

        private static final String INSERT_FILE_ATTRIBUTES_AND_CONTENT = "INSERT INTO process_log (ID, SPACE, NAMESPACE, MODIFIED, OPERATION_ID, OPERATION_LOG, OPERATION_LOG_NAME) VALUES (?, ?, ?, ?, ?, ?, ?)";
        private OperationLogEntry operationLogEntry;

        public LogDbAppender(OperationLogEntry operationLogEntry, String name, Layout<? extends Serializable> layout) {
            super(name, ALL_FILTER, layout, Boolean.FALSE, null);
            this.operationLogEntry = operationLogEntry;
        }

        @Override
        public void append(LogEvent event) {
            try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT_FILE_ATTRIBUTES_AND_CONTENT)) {
                StringBuilder builder = new StringBuilder();
                String test = loggerContext.getConfiguration().getAppenders().values().stream().findFirst().get().getLayout().toSerializable(event).toString();
                builder.append(test);
                if (event.getSource().getClassName().contains("springframework")) {
                    return;
                }
                System.out.println("TEEEEEE");
                if (event.getLevel().equals(Level.ERROR)) {
//                    for (int i = 0; i < event.getThrown().getStackTrace().length; i++) {
//                        builder.append(event.getThrown().getStackTrace()[i].toString());
//                        builder.append("\n");
//                    }
                }

                OperationLogEntry enhancedWithMessageOperationLogEntry = ImmutableOperationLogEntry.copyOf(operationLogEntry)
                                                                                                   .withId(UUID.randomUUID()
                                                                                                               .toString())
                                                                                                   .withModified(LocalDateTime.now())
                                                                                                   .withOperationLog(builder.toString());

                sqlOperationLogQueryProvider.enhanceInsertOperationLogQuery(enhancedWithMessageOperationLogEntry, statement);
                statement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

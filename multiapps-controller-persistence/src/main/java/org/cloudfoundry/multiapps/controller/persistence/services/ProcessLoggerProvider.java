package org.cloudfoundry.multiapps.controller.persistence.services;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

import jakarta.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.flowable.engine.delegate.DelegateExecution;

@Named("processLoggerProvider")
public class ProcessLoggerProvider {
    static final String LOG_LAYOUT = "#%d{yyyy MM dd HH:mm:ss.SSS}#%d{XXX}#%p#%c#%t#%n%m#%n";
    private static final String DEFAULT_LOG_NAME = "OPERATION";
    private static final String LOG_FILE_EXTENSION = ".log";
    private final LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
    private final PatternLayout patternLayout = PatternLayout.newBuilder()
                                                             .withPattern(LOG_LAYOUT)
                                                             .withConfiguration(loggerContext.getConfiguration())
                                                             .build();

    private final Queue<ProcessLogger> loggerChache = new ConcurrentLinkedQueue<>();

    public ProcessLogger getLogger(DelegateExecution execution) {
        return getLogger(execution, DEFAULT_LOG_NAME);
    }

    public ProcessLogger getLogger(DelegateExecution execution, String logName) {
        return getLogger(execution, logName, loggerContextDel -> patternLayout);
    }

    public ProcessLogger getLogger(DelegateExecution execution, String logName,
                                   Function<LoggerContext, AbstractStringLayout> layoutCreatorFunction) {
        String name = getLoggerName(execution, logName);
        String correlationId = getCorrelationId(execution);
        String spaceId = getSpaceId(execution);
        String activityId = getTaskId(execution);
        String logNameWithExtension = logName + LOG_FILE_EXTENSION;
        AbstractStringLayout layout = layoutCreatorFunction.apply(loggerContext);
        if (correlationId == null || activityId == null) {
            return new NullProcessLogger(spaceId, execution.getProcessInstanceId(), activityId);
        }
        ProcessLogger processLogger = createProcessLogger(spaceId, correlationId, activityId, name, logNameWithExtension, layout);
        loggerChache.add(processLogger);
        return processLogger;
    }

    private String getLoggerName(DelegateExecution execution, String logName) {
        return getCorrelationId(execution) + '.' + logName;
    }

    private String getCorrelationId(DelegateExecution execution) {
        return (String) execution.getVariable(Constants.CORRELATION_ID);
    }

    private String getTaskId(DelegateExecution execution) {
        String taskId = (String) execution.getVariable(Constants.TASK_ID);
        return taskId != null ? taskId : execution.getCurrentActivityId();
    }

    private ProcessLogger createProcessLogger(String spaceId, String correlationId, String activityId, String loggerName, String logName,
                                              AbstractStringLayout patternLayout) {
        OperationLogEntry operationLogEntry = ImmutableOperationLogEntry.builder()
                                                                        .space(spaceId)
                                                                        .operationLogName(logName)
                                                                        .operationId(correlationId)
                                                                        .build();
        return new ProcessLogger(operationLogEntry, loggerName, patternLayout, activityId);
    }

    public List<ProcessLogger> getExistingLoggers(String operationId, String activityId) {
        return loggerChache.stream()
                           .filter(logger -> hasLoggerSpecificProcessIdAndActivityId(operationId, activityId, logger))
                           .toList();
    }

    private boolean hasLoggerSpecificProcessIdAndActivityId(String operationId, String activityId, ProcessLogger logger) {
        return operationId.equals(logger.getOperationLogEntry()
                                        .getOperationId())
            && activityId.equals(logger.getActivityId());
    }

    public void removeProcessLoggerFromCache(ProcessLogger processLogger) {
        loggerChache.remove(processLogger);
    }

    private String getSpaceId(DelegateExecution execution) {
        return (String) execution.getVariable(Constants.VARIABLE_NAME_SPACE_ID);
    }
}
package com.sap.cloud.lm.sl.cf.persistence.services;

import javax.inject.Inject;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.persistence.message.Constants;

public class ProcessLogsPersister {

    @Inject
    private ProcessLoggerProvider processLoggerProvider;
    @Inject
    private ProcessLogsPersistenceService processLogsPersistenceService;

    public void persistLogs(DelegateExecution context) {
        for (ProcessLogger processLogger : processLoggerProvider.getExistingLoggers(getCorrelationId(context), getTaskId(context))) {
            processLogger.persistLogFile(processLogsPersistenceService);
            processLogger.deleteLogFile();
            processLoggerProvider.remove(processLogger);
        }
    }

    private String getCorrelationId(DelegateExecution context) {
        return (String) context.getVariable(Constants.CORRELATION_ID);
    }

    private String getTaskId(DelegateExecution context) {
        String taskId = (String) context.getVariable(Constants.TASK_ID);
        return taskId != null ? taskId : context.getCurrentActivityId();
    }
}

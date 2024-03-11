package com.sap.cloud.lm.sl.cf.persistence.services;

import javax.inject.Inject;
import javax.inject.Named;

import org.flowable.engine.delegate.DelegateExecution;

import com.sap.cloud.lm.sl.cf.persistence.message.Constants;
import com.sap.cloud.lm.sl.common.SLException;

@Named("processLogsPersister")
public class ProcessLogsPersister {

    @Inject
    private ProcessLoggerProvider processLoggerProvider;
    @Inject
    private ProcessLogsPersistenceService processLogsPersistenceService;

    public void persistLogs(DelegateExecution context) {
        for (ProcessLogger processLogger : processLoggerProvider.getExistingLoggers(getCorrelationId(context), getTaskId(context))) {
            persistLogsInDatabase(processLogger);
        }
    }

    private void persistLogsInDatabase(ProcessLogger processLogger) {
        try {
            processLogger.persistLogFile(processLogsPersistenceService);
        } catch (Exception e) {
            throw new SLException(e, e.getMessage());
        } finally {
            processLogger.deleteLogFile();
            processLogger.close();
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

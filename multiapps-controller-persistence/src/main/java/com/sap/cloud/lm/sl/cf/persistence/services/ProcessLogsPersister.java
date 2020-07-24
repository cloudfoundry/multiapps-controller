package com.sap.cloud.lm.sl.cf.persistence.services;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.scheduling.annotation.Async;

@Named("processLogsPersister")
public class ProcessLogsPersister {

    @Inject
    private ProcessLoggerProvider processLoggerProvider;
    @Inject
    private ProcessLogsPersistenceService processLogsPersistenceService;

    @Async("asyncExecutor")
    public void persistLogs(String correlationId, String taskId) {
        for (ProcessLogger processLogger : processLoggerProvider.getExistingLoggers(correlationId, taskId)) {
            processLogger.persistLogFile(processLogsPersistenceService);
            processLogger.deleteLogFile();
            processLoggerProvider.remove(processLogger);
        }
    }

}

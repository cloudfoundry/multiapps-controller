package org.cloudfoundry.multiapps.controller.persistence.services;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.common.SLException;
import org.springframework.scheduling.annotation.Async;

@Named("processLogsPersister")
public class ProcessLogsPersister {

    private final ProcessLoggerProvider processLoggerProvider;
    private final ProcessLogsPersistenceService processLogsPersistenceService;

    @Inject
    public ProcessLogsPersister(ProcessLoggerProvider processLoggerProvider, ProcessLogsPersistenceService processLogsPersistenceService) {
        this.processLoggerProvider = processLoggerProvider;
        this.processLogsPersistenceService = processLogsPersistenceService;
    }

    @Async("asyncExecutor")
    public void persistLogs(String correlationId, String taskId) {
        for (ProcessLogger processLogger : processLoggerProvider.getExistingLoggers(correlationId, taskId)) {
            persistLogsInDatabase(processLogger);
        }
    }

    private void persistLogsInDatabase(ProcessLogger processLogger) {
        try {
            processLogger.persistLogFile(processLogsPersistenceService);
        } catch (Exception e) {
            throw new SLException(e, e.getMessage());
        } finally {
            processLoggerProvider.removeLoggersCache(processLogger);
            processLogger.deleteLogFile();
            processLogger.closeLoggerContext();
        }
    }
}

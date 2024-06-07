package org.cloudfoundry.multiapps.controller.persistence.services;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.common.SLException;
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

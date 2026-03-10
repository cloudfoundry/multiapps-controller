package org.cloudfoundry.multiapps.controller.process.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.persistence.Constants;
import org.cloudfoundry.multiapps.controller.persistence.model.ExternalOperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ImmutableExternalOperationLogEntry;
import org.flowable.engine.delegate.DelegateExecution;

@Named("cloudLoggingServiceLogsProvider")
public class CloudLoggingServiceLogsProvider {
    private static final Map<String, List<ExternalOperationLogEntry>> unsendLogCache = new ConcurrentHashMap<>();
    private static final String DEFAULT_LOG_NAME = "OPERATION";

    public void logMessage(DelegateExecution execution, String logMessage, String level) {
        getLogger(execution, DEFAULT_LOG_NAME, logMessage, level);
    }

    public void getLogger(DelegateExecution execution, String logName, String logMessage, String level) {
        String correlationId = getCorrelationId(execution);

        ExternalOperationLogEntry externalOperationLogEntry = ImmutableExternalOperationLogEntry.builder()
                                                                                                .id(UUID.randomUUID()
                                                                                                        .toString())
                                                                                                .correlationId(correlationId)
                                                                                                .message(logMessage)
                                                                                                .timestamp(LocalDateTime.now()
                                                                                                                        .toString())
                                                                                                .build();

        if (unsendLogCache.containsKey(correlationId)) {
            List<ExternalOperationLogEntry> externalOperationLogEntries = unsendLogCache.get(correlationId);
            externalOperationLogEntries.add(externalOperationLogEntry);
        } else {
            List<ExternalOperationLogEntry> externalOperationLogEntries = new ArrayList<>();
            externalOperationLogEntries.add(externalOperationLogEntry);
            unsendLogCache.put(correlationId, externalOperationLogEntries);
        }
    }

    private String getCorrelationId(DelegateExecution execution) {
        return (String) execution.getVariable(Constants.CORRELATION_ID);
    }

    public List<ExternalOperationLogEntry> getUnsendLogs(String operationId) {
        return unsendLogCache.get(operationId);
    }

    public void removeUnsendLogsFromCache(List<ExternalOperationLogEntry> externalOperationLogEntries) {

    }
}
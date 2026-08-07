package org.cloudfoundry.multiapps.controller.persistence.services.cloudlogging;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.OperationLogEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.ProcessLogsPersistenceService;
import org.cloudfoundry.multiapps.controller.persistence.util.CloudLoggingServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named("unsentProcessLogsProvider")
public class UnsentProcessLogsProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnsentProcessLogsProvider.class);

    private final ProcessLogsPersistenceService processLogsPersistenceService;

    @Inject
    public UnsentProcessLogsProvider(ProcessLogsPersistenceService processLogsPersistenceService) {
        this.processLogsPersistenceService = processLogsPersistenceService;
    }

    public List<OperationLogEntry> getUnsentProcessLogs(LoggingConfiguration loggingConfiguration) {
        try {
            return processLogsPersistenceService.listOperationLogsBySpaceAndOperationId(loggingConfiguration.getMtaSpaceId(),
                                                                                        loggingConfiguration.getOperationId());
        } catch (FileStorageException e) {
            CloudLoggingServiceUtil.logErrorOrThrowExceptionBasedOnFailSafe(loggingConfiguration, LOGGER, e.getMessage());
            return List.of();
        }
    }
}

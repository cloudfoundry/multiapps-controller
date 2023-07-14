package org.cloudfoundry.multiapps.controller.process.jobs;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingProvider;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.SecurityUtil;
import org.cloudfoundry.multiapps.mta.model.AuditableConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudControllerClientImpl;
import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.CloudOperationException;

public abstract class OrphanedDataCleaner<T extends AuditableConfiguration> implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrphanedDataCleaner.class);

    private ApplicationConfiguration configuration;
    protected CloudControllerClient client;
    private boolean executed;

    protected OrphanedDataCleaner(ApplicationConfiguration applicationConfiguration) {
        this.configuration = applicationConfiguration;
        this.executed = false;
    }

    @Override
    public void execute(LocalDateTime expirationTime) {
        if (!executed) {
            LOGGER.info(CleanUpJob.LOG_MARKER, getStartCleanupLogMessage());
            int deletedOrphanedDataCount = deleteOrphanedData();
            LOGGER.info(CleanUpJob.LOG_MARKER, getEndCleanupLogMessage(deletedOrphanedDataCount));
            executed = true;
        }
    }

    protected abstract String getStartCleanupLogMessage();

    protected abstract String getEndCleanupLogMessage(int deletedDataCount);

    private int deleteOrphanedData() {
        List<T> configurationData = getConfigurationData();
        return configurationData.stream()
                                .filter(this::hasNoAssociatedSpace)
                                .peek(this::auditLogDeletion)
                                .map(this::getSpaceId)
                                .distinct()
                                .mapToInt(this::deleteConfigurationDataBySpaceId)
                                .sum();
    }

    protected abstract List<T> getConfigurationData();

    private boolean hasNoAssociatedSpace(T configurationData) {
        String spaceId = getSpaceId(configurationData);
        return !spaceExists(spaceId);
    }

    protected abstract String getSpaceId(T configurationData);

    private boolean spaceExists(String spaceId) {
        if (client == null) {
            initCloudControllerClient();
        }
        try {
            client.getSpace(UUID.fromString(spaceId));
            return true;
        } catch (CloudOperationException e) {
            if (e.getStatusCode()
                 .equals(HttpStatus.NOT_FOUND)) {
                return false;
            }
            LOGGER.error(CleanUpJob.LOG_MARKER, "Could not get space by uuid", e);
            // will skip deletion of data
            return true;
        }
    }

    protected abstract int deleteConfigurationDataBySpaceId(String spaceId);

    protected void initCloudControllerClient() {
        CloudCredentials cloudCredentials = new CloudCredentials(configuration.getGlobalAuditorUser(),
                                                                 configuration.getGlobalAuditorPassword(),
                                                                 SecurityUtil.CLIENT_ID,
                                                                 SecurityUtil.CLIENT_SECRET,
                                                                 configuration.getGlobalAuditorOrigin());

        client = new CloudControllerClientImpl(configuration.getControllerUrl(), cloudCredentials, configuration.shouldSkipSslValidation());
        client.login();
    }

    private void auditLogDeletion(T configurationData) {
        AuditLoggingProvider.getFacade()
                            .logConfigDelete(configurationData);
    }

}

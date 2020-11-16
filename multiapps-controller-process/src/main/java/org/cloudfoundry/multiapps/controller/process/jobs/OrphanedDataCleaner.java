package org.cloudfoundry.multiapps.controller.process.jobs;

import java.util.Date;
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
    private static final String AUTH_ORIGIN = "uaa";

    private ApplicationConfiguration configuration;
    protected CloudControllerClient client;
    private boolean executed;

    public OrphanedDataCleaner(ApplicationConfiguration applicationConfiguration) {
        this.configuration = applicationConfiguration;
        this.executed = false;
    }

    @Override
    public void execute(Date expirationTime) {
        if (!executed) {
            LOGGER.debug(CleanUpJob.LOG_MARKER, getStartCleanupLogMessage());
            deleteOrphanedData();
            LOGGER.debug(CleanUpJob.LOG_MARKER, getEndCleanupLogMessage());
            executed = true;
        }
    }

    protected abstract String getStartCleanupLogMessage();

    protected abstract String getEndCleanupLogMessage();

    private void deleteOrphanedData() {
        List<T> configurationData = getConfigurationData();
        configurationData.stream()
                         .filter(this::hasNoAssociatedSpace)
                         .peek(this::auditLogDeletion)
                         .map(this::getSpaceId)
                         .distinct()
                         .forEach(this::deleteConfigurationDataBySpaceId);
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

    protected abstract void deleteConfigurationDataBySpaceId(String spaceId);

    protected void initCloudControllerClient() {
        CloudCredentials cloudCredentials = new CloudCredentials(configuration.getGlobalAuditorUser(),
                                                                 configuration.getGlobalAuditorPassword(),
                                                                 SecurityUtil.CLIENT_ID,
                                                                 SecurityUtil.CLIENT_SECRET,
                                                                 AUTH_ORIGIN);

        client = new CloudControllerClientImpl(configuration.getControllerUrl(), cloudCredentials, configuration.shouldSkipSslValidation());
        client.login();
    }

    private void auditLogDeletion(T configurationData) {
        AuditLoggingProvider.getFacade()
                            .logConfigDelete(configurationData);
    }

}

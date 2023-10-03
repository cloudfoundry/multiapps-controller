package org.cloudfoundry.multiapps.controller.process.jobs;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingProvider;
import org.cloudfoundry.multiapps.controller.core.cf.OAuthClientFactory;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.SecurityUtil;
import org.cloudfoundry.multiapps.mta.model.AuditableConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableCloudFoundryClientFactory;
import com.sap.cloudfoundry.client.facade.rest.CloudSpaceClient;

public abstract class OrphanedDataCleaner<T extends AuditableConfiguration> implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrphanedDataCleaner.class);

    private final ApplicationConfiguration configuration;
    private final OAuthClientFactory oAuthClientFactory;
    protected CloudSpaceClient spaceClient;
    private boolean executed;

    protected OrphanedDataCleaner(ApplicationConfiguration applicationConfiguration,
                                  OAuthClientFactory oAuthClientFactory) {
        this.configuration = applicationConfiguration;
        this.oAuthClientFactory = oAuthClientFactory;
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
        if (spaceClient == null) {
            initSpaceClient();
        }
        try {
            spaceClient.getSpace(UUID.fromString(spaceId));
            return true;
        } catch (CloudOperationException e) {
            if (HttpStatus.NOT_FOUND == e.getStatusCode()) {
                return false;
            }
            LOGGER.error(CleanUpJob.LOG_MARKER, "Could not get space with uuid " + spaceId, e);
            // will skip deletion of data
            return true;
        }
    }

    protected abstract int deleteConfigurationDataBySpaceId(String spaceId);

    protected void initSpaceClient() {
        CloudCredentials cloudCredentials = new CloudCredentials(configuration.getGlobalAuditorUser(),
                                                                 configuration.getGlobalAuditorPassword(),
                                                                 SecurityUtil.CLIENT_ID,
                                                                 SecurityUtil.CLIENT_SECRET,
                                                                 configuration.getGlobalAuditorOrigin());
        var clientFactory = ImmutableCloudFoundryClientFactory.builder()
                                                              .connectTimeout(Duration.ofMinutes(5))
                                                              .responseTimeout(Duration.ofMinutes(5))
                                                              .connectionPoolSize(1)
                                                              .threadPoolSize(1)
                                                              .build();
        var oauthClient = oAuthClientFactory.createOAuthClient();
        oauthClient.init(cloudCredentials);
        spaceClient = clientFactory.createSpaceClient(configuration.getControllerUrl(), oauthClient, Collections.emptyMap());
    }

    private void auditLogDeletion(T configurationData) {
        AuditLoggingProvider.getFacade()
                            .logConfigDelete(configurationData);
    }

}

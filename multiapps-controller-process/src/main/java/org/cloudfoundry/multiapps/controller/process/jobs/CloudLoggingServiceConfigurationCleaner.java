package org.cloudfoundry.multiapps.controller.process.jobs;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.cloudfoundry.multiapps.controller.client.facade.CloudCredentials;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.adapters.ImmutableCloudFoundryClientFactory;
import org.cloudfoundry.multiapps.controller.client.facade.rest.CloudSpaceClient;
import org.cloudfoundry.multiapps.controller.core.cf.OAuthClientFactory;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.SecurityUtil;
import org.cloudfoundry.multiapps.controller.persistence.model.LoggingConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.services.CloudLoggingServiceConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

@Named
public class CloudLoggingServiceConfigurationCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudLoggingServiceConfigurationCleaner.class);

    private final CloudLoggingServiceConfigurationService cloudLoggingServiceConfigurationService;
    private final ApplicationConfiguration applicationConfiguration;
    private final OAuthClientFactory oAuthClientFactory;

    @Inject
    public CloudLoggingServiceConfigurationCleaner(CloudLoggingServiceConfigurationService cloudLoggingServiceConfigurationService,
                                                   ApplicationConfiguration applicationConfiguration,
                                                   OAuthClientFactory oAuthClientFactory) {
        this.cloudLoggingServiceConfigurationService = cloudLoggingServiceConfigurationService;
        this.applicationConfiguration = applicationConfiguration;
        this.oAuthClientFactory = oAuthClientFactory;
    }

    @Override
    public void execute(LocalDateTime expirationTime) {
        LOGGER.info(CleanUpJob.LOG_MARKER, "Starting cloud logging service configuration cleanup...");
        CloudSpaceClient spaceClient = createSpaceClient();
        List<LoggingConfiguration> configurations = cloudLoggingServiceConfigurationService.getAllCloudLoggingServiceConfigurations();
        int deleted = 0;
        for (LoggingConfiguration configuration : configurations) {
            if (isMtaSpaceDeleted(configuration, spaceClient)) {
                LOGGER.info(CleanUpJob.LOG_MARKER,
                            MessageFormat.format("Deleting cloud logging configuration {0} because its MTA space {1} no longer exists",
                                                 configuration.getId(), configuration.getMtaSpaceId()));
                cloudLoggingServiceConfigurationService.deleteCloudLoggingServiceConfiguration(configuration.getId());
                deleted++;
            }
        }
        LOGGER.info(CleanUpJob.LOG_MARKER,
                    MessageFormat.format("Cloud logging service configuration cleanup finished, deleted {0} entries", deleted));
    }

    private boolean isMtaSpaceDeleted(LoggingConfiguration configuration, CloudSpaceClient spaceClient) {
        String mtaSpaceId = configuration.getMtaSpaceId();
        if (mtaSpaceId == null) {
            return false;
        }
        try {
            spaceClient.getSpace(UUID.fromString(mtaSpaceId));
            return false;
        } catch (CloudOperationException e) {
            if (HttpStatus.NOT_FOUND == e.getStatusCode()) {
                return true;
            }
            LOGGER.error(CleanUpJob.LOG_MARKER,
                         MessageFormat.format("Could not check space {0}, skipping deletion of configuration {1}", mtaSpaceId,
                                              configuration.getId()),
                         e);
            return false;
        }
    }

    private CloudSpaceClient createSpaceClient() {
        CloudCredentials cloudCredentials = new CloudCredentials(applicationConfiguration.getGlobalAuditorUser(),
                                                                 applicationConfiguration.getGlobalAuditorPassword(),
                                                                 SecurityUtil.CLIENT_ID, SecurityUtil.CLIENT_SECRET,
                                                                 applicationConfiguration.getGlobalAuditorOrigin());
        
        var clientFactory = ImmutableCloudFoundryClientFactory.builder()
                                                              .connectTimeout(Duration.ofMinutes(5))
                                                              .responseTimeout(Duration.ofMinutes(5))
                                                              .connectionPoolSize(1)
                                                              .threadPoolSize(1)
                                                              .build();
        var oauthClient = oAuthClientFactory.createOAuthClient();
        oauthClient.init(cloudCredentials);
        return clientFactory.createSpaceClient(applicationConfiguration.getControllerUrl(), oauthClient, Collections.emptyMap());
    }
}

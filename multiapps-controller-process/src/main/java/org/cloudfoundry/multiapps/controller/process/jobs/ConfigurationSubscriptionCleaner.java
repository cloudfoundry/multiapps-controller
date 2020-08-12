package org.cloudfoundry.multiapps.controller.process.jobs;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerClientImpl;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingProvider;
import org.cloudfoundry.multiapps.controller.core.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.core.persistence.service.ConfigurationSubscriptionService;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Named
@Order(40)
public class ConfigurationSubscriptionCleaner implements Cleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationSubscriptionCleaner.class);
    private static final String AUTH_ORIGIN = "uaa";

    protected ApplicationConfiguration configuration;
    protected ConfigurationSubscriptionService configurationSubscriptionService;
    protected CloudControllerClient cfClient;
    private boolean executed;

    @Inject
    public ConfigurationSubscriptionCleaner(ApplicationConfiguration applicationConfiguration, ConfigurationSubscriptionService configurationSubscriptionService) {
        this.configuration = applicationConfiguration;
        this.configurationSubscriptionService = configurationSubscriptionService;
        this.executed = false;
    }

    @Override
    public void execute(Date expirationTime) {
        if (!executed) {
            LOGGER.debug(CleanUpJob.LOG_MARKER, "Deleting orphaned configuration subscriptions...");
            deleteOrphanedConfigurationSubscriptions();
            LOGGER.debug(CleanUpJob.LOG_MARKER, "Orphaned configuration subscriptions deleted");
            executed = true;
        }
    }

    private void deleteOrphanedConfigurationSubscriptions() {
        List<ConfigurationSubscription> configurationSubscriptions = configurationSubscriptionService.createQuery()
                                                                                                     .list();
        configurationSubscriptions.stream()
                                  .filter(this::hasNoAssociatedSpace)
                                  .peek(this::auditLogDeletion)
                                  .map(ConfigurationSubscription::getSpaceId)
                                  .distinct()
                                  .forEach(this::deleteConfigurationSubscriptionsBySpaceId);
    }

    private boolean hasNoAssociatedSpace(ConfigurationSubscription configurationSubscription) {
        String spaceId = configurationSubscription.getSpaceId();
        return !spaceExists(spaceId);
    }

    private boolean spaceExists(String spaceId) {
        if (cfClient == null) {
            initCloudControllerClient();
        }
        try {
            cfClient.getSpace(UUID.fromString(spaceId));
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

    private void deleteConfigurationSubscriptionsBySpaceId(String spaceId) {
        configurationSubscriptionService.createQuery()
                                        .deleteAll(spaceId);
    }

    private void initCloudControllerClient() {
        CloudCredentials cloudCredentials = new CloudCredentials(configuration.getGlobalAuditorUser(),
                                                                 configuration.getGlobalAuditorPassword(),
                                                                 SecurityUtil.CLIENT_ID,
                                                                 SecurityUtil.CLIENT_SECRET,
                                                                 AUTH_ORIGIN);

        cfClient = new CloudControllerClientImpl(configuration.getControllerUrl(), cloudCredentials,
                                                 configuration.shouldSkipSslValidation());
        cfClient.login();
    }

    private void auditLogDeletion(ConfigurationSubscription configurationSubscription) {
        AuditLoggingProvider.getFacade()
                            .logConfigDelete(configurationSubscription);
    }
}

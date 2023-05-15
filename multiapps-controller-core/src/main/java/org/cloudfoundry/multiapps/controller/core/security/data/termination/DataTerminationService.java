package org.cloudfoundry.multiapps.controller.core.security.data.termination;

import static java.text.MessageFormat.format;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.api.model.Operation;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingProvider;
import org.cloudfoundry.multiapps.controller.core.cf.clients.CFOptimizedEventGetter;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.SafeExecutor;
import org.cloudfoundry.multiapps.controller.core.util.SecurityUtil;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationSubscriptionService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.persistence.services.OperationService;
import org.cloudfoundry.multiapps.mta.model.AuditableConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloudfoundry.client.facade.CloudControllerClientImpl;
import com.sap.cloudfoundry.client.facade.CloudCredentials;

@Named
public class DataTerminationService {

    private static final String SPACE_DELETE_EVENT_TYPE = "audit.space.delete-request";
    private static final int NUMBER_OF_DAYS_OF_EVENTS = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(DataTerminationService.class);
    private static final SafeExecutor SAFE_EXECUTOR = new SafeExecutor(DataTerminationService::log);

    @Inject
    private ConfigurationEntryService configurationEntryService;
    @Inject
    private ConfigurationSubscriptionService configurationSubscriptionService;
    @Inject
    private OperationService operationService;
    @Inject
    private FileService fileService;
    @Inject
    private ApplicationConfiguration configuration;

    public void deleteOrphanUserData() {
        assertGlobalAuditorCredentialsExist();
        List<String> spaceEventsToBeDeleted = getSpaceDeleteEvents();
        for (String spaceId : spaceEventsToBeDeleted) {
            SAFE_EXECUTOR.execute(() -> deleteConfigurationSubscriptionOrphanData(spaceId));
            SAFE_EXECUTOR.execute(() -> deleteConfigurationEntryOrphanData(spaceId));
            SAFE_EXECUTOR.execute(() -> deleteUserOperationsOrphanData(spaceId));
        }
        if (!spaceEventsToBeDeleted.isEmpty()) {
            SAFE_EXECUTOR.execute(() -> deleteSpaceIdsLeftovers(spaceEventsToBeDeleted));
        }
    }

    private void assertGlobalAuditorCredentialsExist() {
        if (configuration.getGlobalAuditorUser() == null || configuration.getGlobalAuditorPassword() == null) {
            throw new IllegalStateException(Messages.MISSING_GLOBAL_AUDITOR_CREDENTIALS);
        }
    }

    private List<String> getSpaceDeleteEvents() {
        CFOptimizedEventGetter cfOptimizedEventGetter = getCfOptimizedEventGetter();
        List<String> spaceDeleteEvents = cfOptimizedEventGetter.findEvents(SPACE_DELETE_EVENT_TYPE,
                                                                           getDateBeforeDays(NUMBER_OF_DAYS_OF_EVENTS));
        LOGGER.info(MessageFormat.format(Messages.RECENT_DELETE_SPACE_REQUEST_EVENTS, spaceDeleteEvents.size()));
        return spaceDeleteEvents;
    }

    protected CFOptimizedEventGetter getCfOptimizedEventGetter() {
        CloudControllerClientImpl cfClient = getCFClient();
        return new CFOptimizedEventGetter(cfClient);
    }

    private CloudControllerClientImpl getCFClient() {
        CloudCredentials cloudCredentials = new CloudCredentials(configuration.getGlobalAuditorUser(),
                                                                 configuration.getGlobalAuditorPassword(),
                                                                 SecurityUtil.CLIENT_ID,
                                                                 SecurityUtil.CLIENT_SECRET,
                                                                 configuration.getGlobalAuditorOrigin());

        CloudControllerClientImpl cfClient = new CloudControllerClientImpl(configuration.getControllerUrl(),
                                                                           cloudCredentials,
                                                                           configuration.shouldSkipSslValidation());
        cfClient.login();
        return cfClient;
    }

    private String getDateBeforeDays(int numberOfDays) {
        ZonedDateTime dateBeforeTwoDays = ZonedDateTime.now()
                                                       .minus(Duration.ofDays(numberOfDays));
        String result = DateTimeFormatter.ISO_INSTANT
                                         .format(dateBeforeTwoDays);
        LOGGER.info(MessageFormat.format(Messages.PURGE_DELETE_REQUEST_SPACE_FROM_CONFIGURATION_TABLES, result));
        return result;
    }

    private void deleteConfigurationSubscriptionOrphanData(String spaceId) {
        List<ConfigurationSubscription> configurationSubscriptions = configurationSubscriptionService.createQuery()
                                                                                                     .spaceId(spaceId)
                                                                                                     .list();
        if (configurationSubscriptions.isEmpty()) {
            return;
        }
        auditLogDeletion(configurationSubscriptions);
        configurationSubscriptionService.createQuery()
                                        .deleteAll(spaceId);
    }

    private void auditLogDeletion(List<? extends AuditableConfiguration> configurationEntities) {
        for (AuditableConfiguration configurationEntity : configurationEntities) {
            AuditLoggingProvider.getFacade()
                                .logConfigDelete(configurationEntity);
        }
    }

    private void deleteConfigurationEntryOrphanData(String spaceId) {
        List<ConfigurationEntry> configurationEntities = configurationEntryService.createQuery()
                                                                                  .spaceId(spaceId)
                                                                                  .list();
        if (configurationEntities.isEmpty()) {
            return;
        }
        auditLogDeletion(configurationEntities);
        configurationEntryService.createQuery()
                                 .deleteAll(spaceId);
    }

    private void deleteUserOperationsOrphanData(String deleteEventSpaceId) {
        List<Operation> operationsToBeDeleted = operationService.createQuery()
                                                                .spaceId(deleteEventSpaceId)
                                                                .list();
        auditLogDeletion(operationsToBeDeleted);
        operationService.createQuery()
                        .spaceId(deleteEventSpaceId)
                        .delete();
    }

    private void deleteSpaceIdsLeftovers(List<String> spaceIds) {
        try {
            fileService.deleteBySpaceIds(spaceIds);
        } catch (FileStorageException e) {
            throw new SLException(e, Messages.COULD_NOT_DELETE_SPACEIDS_LEFTOVERS);
        }
    }

    private static void log(Exception e) {
        LOGGER.error(format(Messages.ERROR_DURING_DATA_TERMINATION_0, e.getMessage()), e);
    }

}
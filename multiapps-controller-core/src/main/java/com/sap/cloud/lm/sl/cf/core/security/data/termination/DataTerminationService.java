package com.sap.cloud.lm.sl.cf.core.security.data.termination;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClientImpl;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.mta.model.AuditableConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.Messages;
import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.clients.CFOptimizedEventGetter;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationSubscriptionService;
import com.sap.cloud.lm.sl.cf.core.persistence.service.OperationService;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.SecurityUtil;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;

@Named
public class DataTerminationService {

    private static final String AUTH_ORIGIN = "uaa";
    private static final String SPACE_DELETE_EVENT_TYPE = "audit.space.delete-request";
    private static final int NUMBER_OF_DAYS_OF_EVENTS = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(DataTerminationService.class);

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
        List<String> deleteSpaceEventsToBeDeleted = getDeleteSpaceEvents();
        for (String spaceId : deleteSpaceEventsToBeDeleted) {
            deleteConfigurationSubscriptionOrphanData(spaceId);
            deleteConfigurationEntryOrphanData(spaceId);
            deleteUserOperationsOrphanData(spaceId);
            deleteSpaceLeftovers(spaceId);
        }
    }

    private void assertGlobalAuditorCredentialsExist() {
        if (configuration.getGlobalAuditorUser() == null || configuration.getGlobalAuditorPassword() == null) {
            throw new IllegalStateException(Messages.MISSING_GLOBAL_AUDITOR_CREDENTIALS);
        }
    }

    private List<String> getDeleteSpaceEvents() {
        CFOptimizedEventGetter cfOptimizedEventGetter = getCfOptimizedEventGetter();
        return cfOptimizedEventGetter.findEvents(SPACE_DELETE_EVENT_TYPE, getDateBeforeDays(NUMBER_OF_DAYS_OF_EVENTS));
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
                                                                 AUTH_ORIGIN);

        CloudControllerClientImpl cfClient = new CloudControllerClientImpl(configuration.getControllerUrl(),
                                                                           cloudCredentials,
                                                                           configuration.shouldSkipSslValidation());
        cfClient.login();
        return cfClient;
    }

    private String getDateBeforeDays(int numberOfDays) {
        ZonedDateTime dateBeforeTwoDays = ZonedDateTime.now()
                                                       .minus(Duration.ofDays(numberOfDays));
        String result = DateTimeFormatter.ofPattern("yyyy-MM-dd")
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

    private void deleteSpaceLeftovers(String spaceId) {
        try {
            fileService.deleteBySpace(spaceId);
        } catch (FileStorageException e) {
            throw new SLException(e, Messages.COULD_NOT_DELETE_SPACE_LEFTOVERS);
        }
    }

}
package com.sap.cloud.lm.sl.cf.core.security.data.termination;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.time.DateUtils;
import org.cloudfoundry.client.lib.CloudControllerClientImpl;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.clients.CFOptimizedEventGetter;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationSubscriptionDao;
import com.sap.cloud.lm.sl.cf.core.dao.OperationDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.OperationFilter;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.SecurityUtil;
import com.sap.cloud.lm.sl.cf.persistence.services.FileService;
import com.sap.cloud.lm.sl.cf.persistence.services.FileStorageException;
import com.sap.cloud.lm.sl.cf.web.api.model.Operation;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.AuditableConfiguration;

@Component
public class DataTerminationService {

    private static final String SPACE_DELETE_EVENT_TYPE = "audit.space.delete-request";
    private static final int GET_EVENTS_DAYS_BEFORE = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(DataTerminationService.class);

    @Inject
    private ConfigurationEntryDao entryDao;

    @Inject
    private ConfigurationSubscriptionDao subscriptionDao;

    @Inject
    private OperationDao operationDao;

    @Inject
    private FileService fileService;

    @Inject
    private ApplicationConfiguration configuration;

    public void deleteOrphanUserData() {
        List<String> deleteSpaceEventsToBeDeleted = getDeleteSpaceEvents();
        for (String spaceId : deleteSpaceEventsToBeDeleted) {
            deleteConfigurationSubscriptionOrphanData(spaceId);
            deleteConfigurationEntryOrphanData(spaceId);
            deleteUserOperationsOrphanData(spaceId);
            deleteSpaceLeftovers(spaceId);
        }
    }

    private void deleteSpaceLeftovers(String spaceId) {
        try {
            fileService.deleteBySpace(spaceId);
        } catch (FileStorageException e) {
            throw new SLException(e, Messages.COULD_NOT_DELETE_SPACE_LEFTOVERS);
        }
    }

    private void deleteUserOperationsOrphanData(String deleteEventSpaceId) {
        OperationFilter operationFilter = new OperationFilter.Builder().spaceId(deleteEventSpaceId)
            .build();
        List<Operation> operationsToBeDeleted = operationDao.find(operationFilter);
        List<String> result = operationsToBeDeleted.stream()
            .map(Operation::getProcessId)
            .collect(Collectors.toList());
        auditLogDeletion(operationsToBeDeleted);
        operationDao.removeAll(result);
    }

    private void deleteConfigurationSubscriptionOrphanData(String spaceId) {
        List<ConfigurationSubscription> configurationSubscriptions = subscriptionDao.findAll(null, null, spaceId, null);
        if (configurationSubscriptions.isEmpty()) {
            return;
        }
        auditLogDeletion(configurationSubscriptions);
        subscriptionDao.removeAll(configurationSubscriptions);
    }

    private void deleteConfigurationEntryOrphanData(String spaceId) {
        List<ConfigurationEntry> configurationEntities = entryDao.find(spaceId);
        if (configurationEntities.isEmpty()) {
            return;
        }
        auditLogDeletion(configurationEntities);
        entryDao.removeAll(configurationEntities);
    }

    private void auditLogDeletion(List<? extends AuditableConfiguration> configurationEntities) {
        for (AuditableConfiguration configuration : configurationEntities) {
            AuditLoggingProvider.getFacade()
                .logConfigDelete(configuration);
        }
    }

    protected CloudControllerClientImpl getCFClient() {
        CloudCredentials cloudCredentials = new CloudCredentials(configuration.getGlobalAuditorUser(),
            configuration.getGlobalAuditorPassword(), SecurityUtil.CLIENT_ID, SecurityUtil.CLIENT_SECRET);

        CloudControllerClientImpl cfClient = new CloudControllerClientImpl(cloudCredentials, configuration.getTargetURL(),
            configuration.shouldSkipSslValidation());
        cfClient.login();
        return cfClient;
    }

    private List<String> getDeleteSpaceEvents() {
        CloudControllerClientImpl cfClient = getCFClient();
        CFOptimizedEventGetter cfOptimizedEventGetter = new CFOptimizedEventGetter(cfClient);
        return cfOptimizedEventGetter.findEvents(SPACE_DELETE_EVENT_TYPE, getDateBeforeTwoDays());
    }

    private String getDateBeforeTwoDays() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        long currentDateInMillis = new Date().getTime();
        long timeInMillisBeforeTwoDays = currentDateInMillis - GET_EVENTS_DAYS_BEFORE * DateUtils.MILLIS_PER_DAY;
        Date dateBeforeTwoDays = new Date(timeInMillisBeforeTwoDays);
        String result = sdf.format(dateBeforeTwoDays);
        LOGGER.info(Messages.PURGE_DELETE_REQUEST_SPACE_FROM_CONFIGURATION_TABLES, result);
        return result;
    }
}

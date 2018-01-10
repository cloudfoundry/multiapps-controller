package com.sap.cloud.lm.sl.cf.core.security.data.termination;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.time.DateUtils;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.clients.CFOptimizedEventGetter;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationSubscriptionDao;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.util.Configuration;
import com.sap.cloud.lm.sl.cf.core.util.SecurityUtil;

@Component
public class DataTerminationService {

    private static final String SPACE_DELETE_EVENT_TYPE = "audit.space.delete-request";
    private static final int GET_EVENTS_DAYS_BEFORE = 1;
    private CFOptimizedEventGetter cfOptimizedEventGetter;
    private static final Logger LOGGER = LoggerFactory.getLogger(DataTerminationService.class);

    @Inject
    private ConfigurationEntryDao entryDao;

    @Inject
    private ConfigurationSubscriptionDao subscriptionDao;

    public void deleteOrphanUserData() {
        List<String> deleteSpaceEventsToBeDeleted = getDeleteSpaceEvents();
        for (String spaceId : deleteSpaceEventsToBeDeleted) {
            
            deleteConfigurationSubscriptionOrphanData(spaceId);
            AuditLoggingProvider.getFacade().logConfigDelete(
                "Configuration subscription entry with spaceId:" + spaceId + " has been deleted from ConfigurationSubscription table");
            deleteConfigurationEntryOrphanData(spaceId);
            AuditLoggingProvider.getFacade().logConfigDelete(
                "Configuration entry with spaceId:" + spaceId + " has been deleted from ConfigurationEntry table");
        }
    }

    private void deleteConfigurationSubscriptionOrphanData(String spaceId) {
        List<ConfigurationSubscription> configurationSubscriptions = subscriptionDao.findAll(null, null, spaceId, null);
        if(configurationSubscriptions.isEmpty()){
            return;
        }
        subscriptionDao.removeAll(configurationSubscriptions);
    }

    private void deleteConfigurationEntryOrphanData(String spaceId) {
        List<ConfigurationEntry> configurationEntities = entryDao.find(spaceId);
        if(configurationEntities.isEmpty()){
           return;
        }
        entryDao.removeAll(configurationEntities);
    }

    protected CloudFoundryClient getCFClient() {

        CloudCredentials cloudCredentials = new CloudCredentials(Configuration.getInstance().getGlobalAuditorUser(),
            Configuration.getInstance().getGlobalAuditorPassword(), SecurityUtil.CLIENT_ID, SecurityUtil.CLIENT_SECRET);

        CloudFoundryClient cfClient = new CloudFoundryClient(cloudCredentials, Configuration.getInstance().getTargetURL(),
            Configuration.getInstance().shouldSkipSslValidation());
        cfClient.login();

        return cfClient;
    }

    private List<String> getDeleteSpaceEvents() {
        CloudFoundryClient cfClient = getCFClient();
        cfOptimizedEventGetter = new CFOptimizedEventGetter(cfClient);
        List<String> events = cfOptimizedEventGetter.findEvents(SPACE_DELETE_EVENT_TYPE, getDateBeforeTwoDays());

        return events;
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

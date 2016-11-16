package com.sap.cloud.lm.sl.cf.core.helpers;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.cloudfoundry.client.lib.CloudFoundryException;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.cf.detect.ApplicationMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationSubscriptionDao;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.Pair;

public class ConfigurationEntriesPurger {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationEntriesPurger.class);

    public static void purge(CloudFoundryOperations client, String org, String space, ConfigurationEntryDao entryDao,
        ConfigurationSubscriptionDao subscriptionDao) {

        String targetSpace = getTargetSpace(org, space);
        String targetId = new ClientHelper(client).computeSpaceId(org, space);
        List<CloudApplication> apps = getApplications(client);
        purgeConfigurationSubscriptions(targetId, apps, subscriptionDao);
        purgeConfigurationEntries(targetSpace, apps, entryDao);
    }

    private static void purgeConfigurationSubscriptions(String targetId, List<CloudApplication> apps,
        ConfigurationSubscriptionDao subscriptionDao) {
        LOGGER.info(MessageFormat.format(Messages.PURGING_SUBSCRIPTIONS, targetId));

        List<ConfigurationSubscription> configurationSubscriptions = getConfigurationSubscriptions(subscriptionDao, targetId);
        Map<String, Long> subscriptionsMap = createSubscriptionsMap(configurationSubscriptions);
        List<String> appNames = createAppNamesList(apps);
        subscriptionsMap.entrySet().forEach(subscription -> purgeSubscription(subscription, appNames, subscriptionDao));
    }

    private static Map<String, Long> createSubscriptionsMap(List<ConfigurationSubscription> configurationSubscriptions) {
        Map<String, Long> subscriptionsMap = new HashMap<>();
        configurationSubscriptions.forEach(subscription -> subscriptionsMap.put(subscription.getAppName(), subscription.getId()));
        return subscriptionsMap;
    }

    private static List<String> createAppNamesList(List<CloudApplication> apps) {
        List<String> appNames = new ArrayList<>();
        apps.forEach(app -> appNames.add(app.getName()));
        return appNames;
    }

    private static void purgeSubscription(Entry<String, Long> subscription, List<String> appNames, ConfigurationSubscriptionDao dao) {
        if (!appNames.contains(subscription.getKey())) {
            LOGGER.debug(MessageFormat.format(Messages.DELETING_SUBSCRIPTION, subscription.getValue()));
            dao.remove(subscription.getValue());
        }
    }

    private static void purgeConfigurationEntries(String targetSpace, List<CloudApplication> apps, ConfigurationEntryDao entryDao) {
        LOGGER.info(MessageFormat.format(Messages.PURGING_ENTRIES, targetSpace));

        List<ConfigurationEntry> configurationEntries = getConfigurationEntries(entryDao, targetSpace);
        Set<String> actualEntryIds = getCurrentConfigurationEntryIds(apps);
        configurationEntries.forEach(entry -> purgeEntry(entry, actualEntryIds, entryDao));
    }

    private static Set<String> getCurrentConfigurationEntryIds(List<CloudApplication> apps) {
        Set<String> configurationEntryIds = new HashSet<>();
        for (CloudApplication app : apps) {
            ApplicationMtaMetadata metadata = ApplicationMtaMetadataParser.parseAppMetadata(app);
            if (metadata != null) {
                metadata.getProvidedDependencyNames().forEach(
                    dependency -> configurationEntryIds.add(getConfigurationid(metadata.getMtaMetadata().getId(), dependency)));
            }
        }
        return configurationEntryIds;
    }

    private static void purgeEntry(ConfigurationEntry entry, Set<String> actualEntryIds, ConfigurationEntryDao dao) {
        if (!actualEntryIds.contains(entry.getProviderId())) {
            LOGGER.debug(MessageFormat.format(Messages.DELETING_ENTRY, entry.getId()));
            dao.remove(entry.getId());
        }
    }

    private static List<CloudApplication> getApplications(CloudFoundryOperations client) {
        try {
            return client.getApplications();
        } catch (CloudFoundryException e) {
            throw new SLException(e, Messages.ERROR_GETTING_APPLICATIONS);
        }
    }

    private static String getTargetSpace(String org, String space) {
        return ConfigurationEntriesUtil.computeTargetSpace(new Pair<String, String>(org, space));
    }

    private static String getConfigurationid(String mtaId, String providedDependencyName) {
        return ConfigurationEntriesUtil.computeProviderId(mtaId, providedDependencyName);
    }

    private static List<ConfigurationEntry> getConfigurationEntries(ConfigurationEntryDao entryDao, String targetSpace) {
        return entryDao.find(ConfigurationEntriesUtil.PROVIDER_NID, null, null, targetSpace, null, null);
    }

    private static List<ConfigurationSubscription> getConfigurationSubscriptions(ConfigurationSubscriptionDao subscriptionDao,
        String targetId) {
        return subscriptionDao.findAll(null, null, targetId, null);
    }
}

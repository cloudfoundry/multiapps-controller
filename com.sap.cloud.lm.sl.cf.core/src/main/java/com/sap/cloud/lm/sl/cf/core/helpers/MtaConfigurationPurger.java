package com.sap.cloud.lm.sl.cf.core.helpers;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.Pair;

public class MtaConfigurationPurger {

    private static final Logger LOGGER = LoggerFactory.getLogger(MtaConfigurationPurger.class);

    private CloudFoundryOperations client;
    private ConfigurationEntryDao entryDao;
    private ConfigurationSubscriptionDao subscriptionDao;

    public MtaConfigurationPurger(CloudFoundryOperations client, ConfigurationEntryDao entryDao,
        ConfigurationSubscriptionDao subscriptionDao) {
        this.client = client;
        this.entryDao = entryDao;
        this.subscriptionDao = subscriptionDao;
    }

    public void purge(String org, String space) {
        String targetSpace = getTargetSpace(org, space);
        String targetId = new ClientHelper(client).computeSpaceId(org, space);
        List<CloudApplication> existingApps = getExistingApps();
        purgeConfigurationSubscriptions(targetId, existingApps);
        purgeConfigurationEntries(targetSpace, existingApps);
    }

    private void purgeConfigurationSubscriptions(String targetId, List<CloudApplication> existingApps) {
        LOGGER.info(MessageFormat.format(Messages.PURGING_SUBSCRIPTIONS, targetId));

        Set<String> existingAppNames = getNames(existingApps);
        List<ConfigurationSubscription> subscriptions = getSubscriptions(targetId);
        for (ConfigurationSubscription subscription : subscriptions) {
            if (!existingAppNames.contains(subscription.getAppName())) {
                purgeSubscription(subscription);
            }
        }
    }

    private Set<String> getNames(List<CloudApplication> apps) {
        return apps.stream().map(app -> app.getName()).collect(Collectors.toSet());
    }

    private void purgeSubscription(ConfigurationSubscription subscription) {
        LOGGER.debug(MessageFormat.format(Messages.DELETING_SUBSCRIPTION, subscription.getId()));
        subscriptionDao.remove(subscription.getId());
    }

    private void purgeConfigurationEntries(String targetSpace, List<CloudApplication> apps) {
        LOGGER.info(MessageFormat.format(Messages.PURGING_ENTRIES, targetSpace));

        List<ConfigurationEntry> entries = getConfigurationEntries(targetSpace);
        List<ConfigurationEntry> stillRelevantEntries = getStillRelevantConfigurationEntries(apps);
        for (ConfigurationEntry entry : entries) {
            if (!isStillRelevant(stillRelevantEntries, entry)) {
                purgeConfigurationEntry(entry);
            }
        }
    }

    private boolean isStillRelevant(List<ConfigurationEntry> stillRelevantEntries, ConfigurationEntry entry) {
        return stillRelevantEntries.stream().anyMatch(currentEntry -> haveSameProviderIdAndVersion(currentEntry, entry));
    }

    private boolean haveSameProviderIdAndVersion(ConfigurationEntry entry1, ConfigurationEntry entry2) {
        return entry1.getProviderId().equals(entry2.getProviderId()) && entry1.getProviderVersion().equals(entry2.getProviderVersion());
    }

    private List<ConfigurationEntry> getStillRelevantConfigurationEntries(List<CloudApplication> apps) {
        return apps.stream().flatMap(app -> getStillRelevantConfigurationEntries(app).stream()).collect(Collectors.toList());
    }

    private List<ConfigurationEntry> getStillRelevantConfigurationEntries(CloudApplication app) {
        ApplicationMtaMetadata metadata = ApplicationMtaMetadataParser.parseAppMetadata(app);
        if (metadata == null) {
            return Collections.emptyList();
        }
        return metadata.getProvidedDependencyNames().stream().map(
            providedDependencyName -> toConfigurationEntry(metadata.getMtaMetadata(), providedDependencyName)).collect(Collectors.toList());
    }

    private ConfigurationEntry toConfigurationEntry(DeployedMtaMetadata metadata, String providedDependencyName) {
        return new ConfigurationEntry(null, computeProviderId(metadata, providedDependencyName), metadata.getVersion(), null, null, null);
    }

    private void purgeConfigurationEntry(ConfigurationEntry entry) {
        LOGGER.debug(MessageFormat.format(Messages.DELETING_ENTRY, entry.getId()));
        entryDao.remove(entry.getId());
    }

    private List<CloudApplication> getExistingApps() {
        try {
            return client.getApplications();
        } catch (CloudFoundryException e) {
            throw new SLException(e, Messages.ERROR_GETTING_APPLICATIONS);
        }
    }

    private String getTargetSpace(String org, String space) {
        return ConfigurationEntriesUtil.computeTargetSpace(new Pair<String, String>(org, space));
    }

    private String computeProviderId(DeployedMtaMetadata mtaMetadata, String providedDependencyName) {
        return ConfigurationEntriesUtil.computeProviderId(mtaMetadata.getId(), providedDependencyName);
    }

    private List<ConfigurationEntry> getConfigurationEntries(String targetSpace) {
        return entryDao.find(ConfigurationEntriesUtil.PROVIDER_NID, null, null, targetSpace, null, null);
    }

    private List<ConfigurationSubscription> getSubscriptions(String targetId) {
        return subscriptionDao.findAll(null, null, targetId, null);
    }
}

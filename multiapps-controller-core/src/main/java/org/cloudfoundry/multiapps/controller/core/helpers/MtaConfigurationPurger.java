package org.cloudfoundry.multiapps.controller.core.helpers;

import java.text.MessageFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.client.facade.CloudControllerClient;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.controller.client.facade.rest.CloudSpaceClient;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.auditlogging.MtaConfigurationPurgerAuditLog;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.processor.MtaMetadataParser;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.util.ConfigurationEntriesUtil;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationSubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.cloudfoundry.multiapps.controller.core.cf.metadata.util.MtaMetadataUtil.hasMtaMetadata;

public class MtaConfigurationPurger {

    private static final Logger LOGGER = LoggerFactory.getLogger(MtaConfigurationPurger.class);

    private final MtaConfigurationPurgerAuditLog mtaConfigurationPurgerAuditLog;
    private final CloudControllerClient client;
    private final CloudSpaceClient spaceClient;
    private final ConfigurationEntryService configurationEntryService;
    private final ConfigurationSubscriptionService configurationSubscriptionService;
    private MtaMetadataParser mtaMetadataParser;

    public MtaConfigurationPurger(CloudControllerClient client, CloudSpaceClient spaceClient,
                                  ConfigurationEntryService configurationEntryService,
                                  ConfigurationSubscriptionService configurationSubscriptionService, MtaMetadataParser mtaMetadataParser,
                                  MtaConfigurationPurgerAuditLog mtaConfigurationPurgerAuditLog) {
        this.client = client;
        this.spaceClient = spaceClient;
        this.configurationEntryService = configurationEntryService;
        this.configurationSubscriptionService = configurationSubscriptionService;
        this.mtaMetadataParser = mtaMetadataParser;
        this.mtaConfigurationPurgerAuditLog = mtaConfigurationPurgerAuditLog;
    }

    public void purge(String org, String space) {
        CloudTarget targetSpace = new CloudTarget(org, space);
        String targetId = new ClientHelper(spaceClient).computeSpaceId(org, space);
        List<CloudApplication> existingApps = getExistingApps();
        purgeConfigurationSubscriptions(targetId, existingApps);
        purgeConfigurationEntries(targetSpace, existingApps, targetId);
    }

    private void purgeConfigurationSubscriptions(String spaceId, List<CloudApplication> existingApps) {
        LOGGER.info(MessageFormat.format(Messages.PURGING_SUBSCRIPTIONS, spaceId));

        Set<String> existingAppNames = getNames(existingApps);
        List<ConfigurationSubscription> subscriptions = getSubscriptions(spaceId);
        for (ConfigurationSubscription subscription : subscriptions) {
            if (!existingAppNames.contains(subscription.getAppName())) {
                purgeSubscription(subscription, spaceId);
            }
        }
    }

    private Set<String> getNames(List<CloudApplication> apps) {
        return apps.stream()
                   .map(CloudApplication::getName)
                   .collect(Collectors.toSet());
    }

    private void purgeSubscription(ConfigurationSubscription subscription, String spaceId) {
        LOGGER.debug(MessageFormat.format(Messages.DELETING_SUBSCRIPTION, subscription.getId()));
        mtaConfigurationPurgerAuditLog.logDeleteSubscription(spaceId, subscription);
        configurationSubscriptionService.createQuery()
                                        .id(subscription.getId())
                                        .delete();
    }

    private void purgeConfigurationEntries(CloudTarget targetSpace, List<CloudApplication> apps, String spaceId) {
        LOGGER.info(MessageFormat.format(Messages.PURGING_ENTRIES, targetSpace));

        List<ConfigurationEntry> entries = getConfigurationEntries(targetSpace);
        List<ConfigurationEntry> stillRelevantEntries = getStillRelevantConfigurationEntries(apps);
        for (ConfigurationEntry entry : entries) {
            if (!isStillRelevant(stillRelevantEntries, entry)) {
                purgeConfigurationEntry(entry, spaceId);
            }
        }
    }

    private boolean isStillRelevant(List<ConfigurationEntry> stillRelevantEntries, ConfigurationEntry entry) {
        return stillRelevantEntries.stream()
                                   .anyMatch(currentEntry -> haveSameProviderIdAndVersion(currentEntry, entry));
    }

    private boolean haveSameProviderIdAndVersion(ConfigurationEntry entry1, ConfigurationEntry entry2) {
        return entry1.getProviderId()
                     .equals(entry2.getProviderId()) && entry1.getProviderVersion()
                                                              .equals(entry2.getProviderVersion());
    }

    private List<ConfigurationEntry> getStillRelevantConfigurationEntries(List<CloudApplication> apps) {
        return apps.stream()
                   .flatMap(this::getStillRelevantConfigurationEntries)
                   .collect(Collectors.toList());
    }

    private Stream<ConfigurationEntry> getStillRelevantConfigurationEntries(CloudApplication app) {
        MtaMetadata metadata = getMtaMetadata(app);
        if (metadata == null) {
            return Stream.empty();
        }
        return getDeployedMtaApplication(app).getProvidedDependencyNames()
                                             .stream()
                                             .map(providedDependencyName -> toConfigurationEntry(metadata, providedDependencyName));
    }

    private MtaMetadata getMtaMetadata(CloudApplication app) {
        if (hasMtaMetadata(app)) {
            return mtaMetadataParser.parseMtaMetadata(app);
        }
        return null;
    }

    private DeployedMtaApplication getDeployedMtaApplication(CloudApplication app) {
        return mtaMetadataParser.parseDeployedMtaApplication(app);
    }

    private ConfigurationEntry toConfigurationEntry(MtaMetadata metadata, String providedDependencyName) {
        return new ConfigurationEntry(computeProviderId(metadata, providedDependencyName), metadata.getVersion());
    }

    private void purgeConfigurationEntry(ConfigurationEntry entry, String spaceId) {
        LOGGER.debug(MessageFormat.format(Messages.DELETING_ENTRY, entry.getId()));
        mtaConfigurationPurgerAuditLog.logDeleteEntry(spaceId, entry);
        configurationEntryService.createQuery()
                                 .id(entry.getId())
                                 .delete();
    }

    private List<CloudApplication> getExistingApps() {
        try {
            return client.getApplications();
        } catch (CloudOperationException e) {
            throw new SLException(e, Messages.ERROR_GETTING_APPLICATIONS);
        }
    }

    private String computeProviderId(MtaMetadata mtaMetadata, String providedDependencyName) {
        return ConfigurationEntriesUtil.computeProviderId(mtaMetadata.getId(), providedDependencyName);
    }

    private List<ConfigurationEntry> getConfigurationEntries(CloudTarget targetSpace) {
        return configurationEntryService.createQuery()
                                        .providerNid(ConfigurationEntriesUtil.PROVIDER_NID)
                                        .target(targetSpace)
                                        .list();
    }

    private List<ConfigurationSubscription> getSubscriptions(String spaceId) {
        return configurationSubscriptionService.createQuery()
                                               .spaceId(spaceId)
                                               .list();
    }

}

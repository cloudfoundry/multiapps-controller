package com.sap.cloud.lm.sl.cf.core.helpers;

import static com.sap.cloud.lm.sl.cf.core.cf.metadata.util.MtaMetadataUtil.hasEnvMtaMetadata;
import static com.sap.cloud.lm.sl.cf.core.cf.metadata.util.MtaMetadataUtil.hasMtaMetadata;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.auditlogging.AuditLoggingProvider;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadata;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.EnvMtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.processor.MtaMetadataParser;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationSubscriptionService;
import com.sap.cloud.lm.sl.cf.core.util.ConfigurationEntriesUtil;
import com.sap.cloud.lm.sl.common.SLException;

public class MtaConfigurationPurger {

    private static final Logger LOGGER = LoggerFactory.getLogger(MtaConfigurationPurger.class);

    private final CloudControllerClient client;
    private final ConfigurationEntryService configurationEntryService;
    private final ConfigurationSubscriptionService configurationSubscriptionService;
    private MtaMetadataParser mtaMetadataParser;
    private EnvMtaMetadataParser envMtaMetadataParser;

    public MtaConfigurationPurger(CloudControllerClient client, ConfigurationEntryService configurationEntryService,
                                  ConfigurationSubscriptionService configurationSubscriptionService, MtaMetadataParser mtaMetadataParser,
                                  EnvMtaMetadataParser envMtaMetadataParser) {
        this.client = client;
        this.configurationEntryService = configurationEntryService;
        this.configurationSubscriptionService = configurationSubscriptionService;
        this.mtaMetadataParser = mtaMetadataParser;
        this.envMtaMetadataParser = envMtaMetadataParser;
    }

    public void purge(String org, String space) {
        CloudTarget targetSpace = new CloudTarget(org, space);
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
        return apps.stream()
                   .map(CloudApplication::getName)
                   .collect(Collectors.toSet());
    }

    private void purgeSubscription(ConfigurationSubscription subscription) {
        LOGGER.debug(MessageFormat.format(Messages.DELETING_SUBSCRIPTION, subscription.getId()));
        AuditLoggingProvider.getFacade()
                            .logConfigDelete(subscription);
        configurationSubscriptionService.createQuery()
                                        .id(subscription.getId())
                                        .delete();
    }

    private void purgeConfigurationEntries(CloudTarget targetSpace, List<CloudApplication> apps) {
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
        return stillRelevantEntries.stream()
                                   .anyMatch(currentEntry -> haveSameProviderIdAndVersion(currentEntry, entry));
    }

    private boolean haveSameProviderIdAndVersion(ConfigurationEntry entry1, ConfigurationEntry entry2) {
        return entry1.getProviderId()
                     .equals(entry2.getProviderId())
            && entry1.getProviderVersion()
                     .equals(entry2.getProviderVersion());
    }

    private List<ConfigurationEntry> getStillRelevantConfigurationEntries(List<CloudApplication> apps) {
        return apps.stream()
                   .flatMap(app -> getStillRelevantConfigurationEntries(app).stream())
                   .collect(Collectors.toList());
    }

    private List<ConfigurationEntry> getStillRelevantConfigurationEntries(CloudApplication app) {
        MtaMetadata metadata = getMtaMetadata(app);
        if (metadata == null) {
            return Collections.emptyList();
        }
        return getDeployedMtaApplication(app).getProvidedDependencyNames()
                                             .stream()
                                             .map(providedDependencyName -> toConfigurationEntry(metadata, providedDependencyName))
                                             .collect(Collectors.toList());
    }

    private MtaMetadata getMtaMetadata(CloudApplication app) {
        if (hasMtaMetadata(app)) {
            return mtaMetadataParser.parseMtaMetadata(app);
        } else if (hasEnvMtaMetadata(app)) {
            return envMtaMetadataParser.parseMtaMetadata(app);
        }
        return null;
    }

    private DeployedMtaApplication getDeployedMtaApplication(CloudApplication app) {
        if (hasMtaMetadata(app)) {
            return mtaMetadataParser.parseDeployedMtaApplication(app);
        }
        return envMtaMetadataParser.parseDeployedMtaApplication(app);
    }

    private ConfigurationEntry toConfigurationEntry(MtaMetadata metadata, String providedDependencyName) {
        return new ConfigurationEntry(null,
                                      computeProviderId(metadata, providedDependencyName),
                                      metadata.getVersion(),
                                      null,
                                      null,
                                      null,
                                      null);
    }

    private void purgeConfigurationEntry(ConfigurationEntry entry) {
        LOGGER.debug(MessageFormat.format(Messages.DELETING_ENTRY, entry.getId()));
        AuditLoggingProvider.getFacade()
                            .logConfigDelete(entry);
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

    private List<ConfigurationSubscription> getSubscriptions(String targetId) {
        return configurationSubscriptionService.createQuery()
                                               .spaceId(targetId)
                                               .list();
    }

}

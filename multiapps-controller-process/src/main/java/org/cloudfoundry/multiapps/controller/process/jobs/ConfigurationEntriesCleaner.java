package org.cloudfoundry.multiapps.controller.process.jobs;

import java.text.MessageFormat;
import java.util.List;

import javax.inject.Inject;

import org.cloudfoundry.multiapps.controller.core.cf.OAuthClientFactory;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;

/*
 * This cleaner is heavy because it scans for deleted space events for every configuration entry.
 * Annotations are commented because this cleaner is not necessary to be executed.
 * Uncomment these annotations in case it exists garbage and cleaner should be executed.
 */
//@Named()
//@Order(40)
public class ConfigurationEntriesCleaner extends OrphanedDataCleaner<ConfigurationEntry> {

    private final ConfigurationEntryService configurationEntryService;

    @Inject
    public ConfigurationEntriesCleaner(ApplicationConfiguration applicationConfiguration,
                                       ConfigurationEntryService configurationEntryService,
                                       OAuthClientFactory oAuthClientFactory) {
        super(applicationConfiguration, oAuthClientFactory);
        this.configurationEntryService = configurationEntryService;
    }

    @Override
    protected String getStartCleanupLogMessage() {
        return "Deleting orphaned configuration entries...";
    }

    @Override
    protected String getEndCleanupLogMessage(int deletedEntriesCount) {
        return MessageFormat.format("Orphaned configuration entries deleted: {0}", deletedEntriesCount);
    }

    @Override
    protected List<ConfigurationEntry> getConfigurationData() {
        return configurationEntryService.createQuery()
                                        .list();
    }

    @Override
    protected String getSpaceId(ConfigurationEntry configurationData) {
        return configurationData.getSpaceId();
    }

    @Override
    protected int deleteConfigurationDataBySpaceId(String spaceId) {
        return configurationEntryService.createQuery()
                                        .deleteAll(spaceId);
    }

}

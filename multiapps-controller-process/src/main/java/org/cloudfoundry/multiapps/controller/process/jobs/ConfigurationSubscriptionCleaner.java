package org.cloudfoundry.multiapps.controller.process.jobs;

import java.util.List;

import javax.inject.Inject;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationSubscription;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationSubscriptionService;

/*
 * This cleaner is heavy because it scans for deleted space events for every configuration subscription.
 * Annotations are commented because this cleaner is not necessary to be executed.
 * Uncomment these annotations in case it exists garbage and cleaner should be executed.
 */
//@Named()
//@Order(40)
public class ConfigurationSubscriptionCleaner extends OrphanedDataCleaner<ConfigurationSubscription> {

    private final ConfigurationSubscriptionService configurationSubscriptionService;

    @Inject
    public ConfigurationSubscriptionCleaner(ApplicationConfiguration applicationConfiguration,
                                            ConfigurationSubscriptionService configurationSubscriptionService) {
        super(applicationConfiguration);
        this.configurationSubscriptionService = configurationSubscriptionService;
    }

    @Override
    protected String getStartCleanupLogMessage() {
        return "Deleting orphaned configuration subscriptions...";
    }

    @Override
    protected String getEndCleanupLogMessage() {
        return "Orphaned configuration subscriptions deleted";
    }

    @Override
    protected List<ConfigurationSubscription> getConfigurationData() {
        return configurationSubscriptionService.createQuery()
                                               .list();
    }

    @Override
    protected String getSpaceId(ConfigurationSubscription configurationData) {
        return configurationData.getSpaceId();
    }

    @Override
    protected void deleteConfigurationDataBySpaceId(String spaceId) {
        configurationSubscriptionService.createQuery()
                                        .deleteAll(spaceId);
    }

}

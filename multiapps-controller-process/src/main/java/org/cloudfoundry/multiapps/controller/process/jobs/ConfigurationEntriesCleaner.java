package org.cloudfoundry.multiapps.controller.process.jobs;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.ConfigurationEntry;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;
import org.springframework.core.annotation.Order;

@Named()
@Order(40)
public class ConfigurationEntriesCleaner extends OrphanedDataCleaner<ConfigurationEntry> {

    private final ConfigurationEntryService configurationEntryService;

    @Inject
    public ConfigurationEntriesCleaner(ApplicationConfiguration applicationConfiguration,
                                       ConfigurationEntryService configurationEntryService) {
        super(applicationConfiguration);
        this.configurationEntryService = configurationEntryService;
    }

    @Override
    protected String getStartCleanupLogMessage() {
        return "Deleting orphaned configuration entries...";
    }

    @Override
    protected String getEndCleanupLogMessage() {
        return "Orphaned configuration entries deleted";
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
    protected void deleteConfigurationDataBySpaceId(String spaceId) {
        configurationEntryService.createQuery()
                                 .deleteAll(spaceId);
    }

}

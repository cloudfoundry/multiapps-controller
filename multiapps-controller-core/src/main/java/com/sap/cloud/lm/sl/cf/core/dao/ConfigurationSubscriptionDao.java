package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.ConfigurationSubscriptionDto;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;

@Component
public class ConfigurationSubscriptionDao {

    @Inject
    protected ConfigurationSubscriptionDtoDao dao;

    private static List<ConfigurationSubscription> toConfigurationSubscriptions(List<ConfigurationSubscriptionDto> dtos) {
        return dtos.stream()
                   .map(ConfigurationSubscriptionDto::toConfigurationSubscription)
                   .collect(Collectors.toList());
    }

    public List<ConfigurationSubscription> findAll() {
        return toConfigurationSubscriptions(dao.findAll());
    }

    public ConfigurationSubscription update(long id, ConfigurationSubscription subscription) {
        return dao.update(id, new ConfigurationSubscriptionDto(subscription))
                  .toConfigurationSubscription();
    }

    public ConfigurationSubscription remove(long id) {
        return dao.remove(id)
                  .toConfigurationSubscription();
    }

    public List<ConfigurationSubscription> removeAll(List<ConfigurationSubscription> configurationSubscriptions) {
        for (ConfigurationSubscription configurationSubscription : configurationSubscriptions) {
            dao.remove(configurationSubscription.getId());
        }
        return configurationSubscriptions;
    }

    public ConfigurationSubscription add(ConfigurationSubscription subscription) {
        return dao.add(new ConfigurationSubscriptionDto(subscription))
                  .toConfigurationSubscription();
    }

    public List<ConfigurationSubscription> findAll(String mtaId, String appName, String spaceId, String resourceName) {
        return toConfigurationSubscriptions(dao.findAll(mtaId, appName, spaceId, resourceName));
    }

    public List<ConfigurationSubscription> findAll(List<ConfigurationEntry> entries) {
        return findAll().stream()
                        .filter(subscription -> subscription.matches(entries))
                        .collect(Collectors.toList());
    }

    public List<ConfigurationSubscription> findAll(String spaceGuid) {
        return toConfigurationSubscriptions(dao.findAll(spaceGuid));
    }

    public ConfigurationSubscription find(long id) {
        return dao.find(id)
                  .toConfigurationSubscription();
    }
}

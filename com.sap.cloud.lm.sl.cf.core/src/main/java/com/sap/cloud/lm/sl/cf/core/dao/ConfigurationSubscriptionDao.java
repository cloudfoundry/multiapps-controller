package com.sap.cloud.lm.sl.cf.core.dao;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.dto.persistence.ConfigurationSubscriptionDto;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;

@Component
public class ConfigurationSubscriptionDao extends AbstractDao<ConfigurationSubscription, ConfigurationSubscriptionDto, Long> {

    @Inject
    protected ConfigurationSubscriptionDtoDao dao;

    public List<ConfigurationSubscription> findAll(String mtaId, String appName, String spaceId, String resourceName) {
        return fromDtos(dao.findAll(mtaId, appName, spaceId, resourceName));
    }

    public List<ConfigurationSubscription> findAll(List<ConfigurationEntry> entries) {
        return findAll().stream()
            .filter(subscription -> subscription.matches(entries))
            .collect(Collectors.toList());
    }

    public List<ConfigurationSubscription> findAll(String spaceGuid) {
        return fromDtos(dao.findAll(spaceGuid));
    }

    @Override
    public ConfigurationSubscription find(Long id) {
        return fromDto(dao.findRequired(id));
    }

    @Override
    protected AbstractDtoDao<ConfigurationSubscriptionDto, Long> getDtoDao() {
        return dao;
    }

    @Override
    protected ConfigurationSubscription fromDto(ConfigurationSubscriptionDto configurationSubscriptionDto) {
        return configurationSubscriptionDto != null ? configurationSubscriptionDto.toConfigurationSubscription() : null;
    }

    @Override
    protected ConfigurationSubscriptionDto toDto(ConfigurationSubscription configurationSubscription) {
        return new ConfigurationSubscriptionDto(configurationSubscription);
    }

}

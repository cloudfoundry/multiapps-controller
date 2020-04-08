package com.sap.cloud.lm.sl.cf.core.persistence.query;

import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.persistence.OrderDirection;

public interface ConfigurationEntryQuery extends Query<ConfigurationEntry, ConfigurationEntryQuery> {

    ConfigurationEntryQuery id(Long id);

    ConfigurationEntryQuery providerNid(String providerNid);

    ConfigurationEntryQuery providerId(String providerId);

    ConfigurationEntryQuery target(CloudTarget targetOrg);

    ConfigurationEntryQuery requiredProperties(Map<String, Object> requiredProperties);

    ConfigurationEntryQuery spaceId(String spaceId);
    
    ConfigurationEntryQuery spaceIdNotNull();
    
    ConfigurationEntryQuery spaceIdNull();
    
    ConfigurationEntryQuery content(String content);

    ConfigurationEntryQuery contentIdNull();

    ConfigurationEntryQuery version(String version);

    ConfigurationEntryQuery visibilityTargets(List<CloudTarget> visibilityTargets);

    ConfigurationEntryQuery mtaId(String mtaId);
    
    ConfigurationEntryQuery orderById(OrderDirection orderDirection);
    
    int deleteAll(String spaceId);

}
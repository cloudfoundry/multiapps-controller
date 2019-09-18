package com.sap.cloud.lm.sl.cf.core.persistence.query;

import java.util.List;

import com.sap.cloud.lm.sl.cf.core.model.ConfigurationEntry;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;

public interface ConfigurationSubscriptionQuery extends Query<ConfigurationSubscription, ConfigurationSubscriptionQuery> {

    ConfigurationSubscriptionQuery id(Long id);

    ConfigurationSubscriptionQuery mtaId(String mtaId);

    ConfigurationSubscriptionQuery spaceId(String spaceId);

    ConfigurationSubscriptionQuery appName(String appName);

    ConfigurationSubscriptionQuery resourceName(String resourceName);

    ConfigurationSubscriptionQuery onSelectMatching(List<ConfigurationEntry> entries);

}
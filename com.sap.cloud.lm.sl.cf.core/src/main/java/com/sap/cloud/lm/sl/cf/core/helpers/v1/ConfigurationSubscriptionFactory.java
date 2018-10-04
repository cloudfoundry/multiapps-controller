package com.sap.cloud.lm.sl.cf.core.helpers.v1;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ResolvedConfigurationReference;
import com.sap.cloud.lm.sl.mta.model.v1.DeploymentDescriptor;

public class ConfigurationSubscriptionFactory {

    public List<ConfigurationSubscription> create(DeploymentDescriptor descriptor,
        Map<String, ResolvedConfigurationReference> resolvedResources, String spaceId) {
        return Collections.emptyList();
    }

}

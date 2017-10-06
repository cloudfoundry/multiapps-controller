package com.sap.cloud.lm.sl.cf.core.helpers.v3_1;

import java.util.List;

import com.sap.cloud.lm.sl.cf.core.resolvers.v3_1.PartialDescriptorReferenceResolver;
import com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.resolvers.v2_0.DescriptorReferenceResolver;

public class ConfigurationSubscriptionFactory extends com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ConfigurationSubscriptionFactory {

    @Override
    protected DescriptorReferenceResolver getPartialDescriptorReferenceResolver(
        com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor descriptor, List<String> dependenciesToIgnore) {
        return new PartialDescriptorReferenceResolver((DeploymentDescriptor) descriptor, dependenciesToIgnore);
    }

}

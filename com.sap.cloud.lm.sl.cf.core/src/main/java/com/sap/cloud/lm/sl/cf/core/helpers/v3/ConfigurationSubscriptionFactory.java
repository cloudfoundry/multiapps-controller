package com.sap.cloud.lm.sl.cf.core.helpers.v3;

import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.model.ResolvedConfigurationReference;
import com.sap.cloud.lm.sl.cf.core.resolvers.v3.PartialDescriptorReferenceResolver;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.RequiredDependency;
import com.sap.cloud.lm.sl.mta.resolvers.v3.DescriptorReferenceResolver;

public class ConfigurationSubscriptionFactory extends com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationSubscriptionFactory {

    public ConfigurationSubscriptionFactory(DeploymentDescriptor descriptor,
                                            Map<String, ResolvedConfigurationReference> resolvedResources) {
        super(descriptor, resolvedResources);
    }

    @Override
    protected DescriptorReferenceResolver getPartialDescriptorReferenceResolver(DeploymentDescriptor descriptor,
                                                                                List<String> dependenciesToIgnore) {
        return new PartialDescriptorReferenceResolver(descriptor, dependenciesToIgnore);
    }

    @Override
    protected boolean shouldCreateSubscription(RequiredDependency dependency) {
        ResolvedConfigurationReference resolvedReference = resolvedResources.get(dependency.getName());
        return super.shouldCreateSubscription(dependency) && resolvedReference.getReference()
                                                                              .isActive();
    }
}

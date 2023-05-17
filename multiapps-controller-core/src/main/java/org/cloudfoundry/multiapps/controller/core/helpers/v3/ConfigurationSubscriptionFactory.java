package org.cloudfoundry.multiapps.controller.core.helpers.v3;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.multiapps.controller.core.model.ResolvedConfigurationReference;
import org.cloudfoundry.multiapps.controller.core.resolvers.v3.PartialDescriptorReferenceResolver;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.resolvers.v3.DescriptorReferenceResolver;

public class ConfigurationSubscriptionFactory
    extends org.cloudfoundry.multiapps.controller.core.helpers.v2.ConfigurationSubscriptionFactory {

    public ConfigurationSubscriptionFactory(DeploymentDescriptor descriptor, Map<String, ResolvedConfigurationReference> resolvedResources,
                                            Set<String> dynamicResolvableParameters) {
        super(descriptor, resolvedResources, dynamicResolvableParameters);
    }

    @Override
    protected DescriptorReferenceResolver getPartialDescriptorReferenceResolver(DeploymentDescriptor descriptor,
                                                                                List<String> dependenciesToIgnore,
                                                                                Set<String> dynamicResolvableParameters) {
        return new PartialDescriptorReferenceResolver(descriptor, dependenciesToIgnore, dynamicResolvableParameters);
    }

    @Override
    protected boolean shouldCreateSubscription(RequiredDependency dependency) {
        ResolvedConfigurationReference resolvedReference = resolvedResources.get(dependency.getName());
        return super.shouldCreateSubscription(dependency) && resolvedReference.getReference()
                                                                              .isActive();
    }
}

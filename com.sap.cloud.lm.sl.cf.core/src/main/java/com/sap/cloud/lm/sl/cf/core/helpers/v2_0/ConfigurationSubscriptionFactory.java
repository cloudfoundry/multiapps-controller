package com.sap.cloud.lm.sl.cf.core.helpers.v2_0;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.dao.filters.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ResolvedConfigurationReference;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.resolvers.v2_0.PartialDescriptorReferenceResolver;
import com.sap.cloud.lm.sl.mta.model.v2_0.Module;
import com.sap.cloud.lm.sl.mta.model.v2_0.RequiredDependency;
import com.sap.cloud.lm.sl.mta.resolvers.v2_0.DescriptorReferenceResolver;

public class ConfigurationSubscriptionFactory extends com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ConfigurationSubscriptionFactory {

    private static final int MTA_MAJOR_SCHEMA_VERSION = 2;

    @Override
    public List<ConfigurationSubscription> create(com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor descriptor,
        Map<String, ResolvedConfigurationReference> resolvedResources, String spaceId) {
        return create((com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor) descriptor, resolvedResources, spaceId);
    }

    public List<ConfigurationSubscription> create(com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor descriptor,
        Map<String, ResolvedConfigurationReference> resolvedResources, String spaceId) {
        List<String> dependenciesToIgnore = new ArrayList<>(resolvedResources.keySet());
        descriptor = getPartialDescriptorReferenceResolver(descriptor, dependenciesToIgnore).resolve();
        List<ConfigurationSubscription> result = new ArrayList<>();
        for (com.sap.cloud.lm.sl.mta.model.v2_0.Module module : descriptor.getModules2_0()) {
            for (RequiredDependency dependency : module.getRequiredDependencies2_0()) {
                if (shouldCreateSubscription(dependency)) {
                    result.add(createSubscription(spaceId, descriptor.getId(), module, dependency, resolvedResources));
                }
            }
        }
        return result;
    }

    protected DescriptorReferenceResolver getPartialDescriptorReferenceResolver(
        com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor descriptor, List<String> dependenciesToIgnore) {
        return new PartialDescriptorReferenceResolver(descriptor, dependenciesToIgnore);
    }

    private ConfigurationSubscription createSubscription(String spaceId, String mtaId, Module module, RequiredDependency dependency,
        Map<String, ResolvedConfigurationReference> resolvedResources) {
        ResolvedConfigurationReference resolvedReference = resolvedResources.get(dependency.getName());
        ConfigurationFilter filter = resolvedReference.getReferenceFilter();
        String appName = (String) module.getParameters()
            .get(SupportedParameters.APP_NAME);
        com.sap.cloud.lm.sl.mta.model.v1_0.Resource resource = resolvedReference.getReference();
        Module adaptedModule = getContainingOneRequiresDependency(module, dependency);

        return ConfigurationSubscription.from(mtaId, spaceId, appName, filter, adaptedModule, resource, getMajorSchemaVersion());
    }

    protected Module getContainingOneRequiresDependency(Module module, RequiredDependency dependency) {
        Module.Builder builder = new Module.Builder();
        builder.setName(module.getName());
        builder.setType(module.getType());
        builder.setPath(module.getPath());
        builder.setDescription(module.getDescription());
        builder.setProperties(module.getProperties());
        builder.setParameters(module.getParameters());
        builder.setProvidedDependencies2_0(module.getProvidedDependencies2_0());
        builder.setRequiredDependencies2_0(Arrays.asList(dependency));
        return builder.build();
    }

    protected int getMajorSchemaVersion() {
        return MTA_MAJOR_SCHEMA_VERSION;
    }

    private boolean shouldCreateSubscription(RequiredDependency dependency) {
        return (boolean) dependency.getParameters()
            .getOrDefault(SupportedParameters.MANAGED, false);
    }

}

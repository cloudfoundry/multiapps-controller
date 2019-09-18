package com.sap.cloud.lm.sl.cf.core.helpers.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

import com.sap.cloud.lm.sl.cf.core.model.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ResolvedConfigurationReference;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.resolvers.v2.PartialDescriptorReferenceResolver;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;
import com.sap.cloud.lm.sl.mta.model.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.Resource;
import com.sap.cloud.lm.sl.mta.resolvers.v2.DescriptorReferenceResolver;

public class ConfigurationSubscriptionFactory {

    private static final int MTA_MAJOR_SCHEMA_VERSION = 2;

    public List<ConfigurationSubscription> create(DeploymentDescriptor descriptor,
                                                  Map<String, ResolvedConfigurationReference> resolvedResources, String spaceId) {
        List<String> dependenciesToIgnore = new ArrayList<>(resolvedResources.keySet());
        descriptor = getPartialDescriptorReferenceResolver(descriptor, dependenciesToIgnore).resolve();
        List<ConfigurationSubscription> result = new ArrayList<>();
        for (Module module : descriptor.getModules()) {
            for (RequiredDependency dependency : module.getRequiredDependencies()) {
                if (shouldCreateSubscription(dependency)) {
                    CollectionUtils.addIgnoreNull(result,
                                                  createSubscription(spaceId, descriptor.getId(), module, dependency, resolvedResources));
                }
            }
        }
        return result;
    }

    protected DescriptorReferenceResolver getPartialDescriptorReferenceResolver(DeploymentDescriptor descriptor,
                                                                                List<String> dependenciesToIgnore) {
        return new PartialDescriptorReferenceResolver(descriptor, dependenciesToIgnore);
    }

    protected ConfigurationSubscription createSubscription(String spaceId, String mtaId, Module module, RequiredDependency dependency,
                                                           Map<String, ResolvedConfigurationReference> resolvedResources) {
        ResolvedConfigurationReference resolvedReference = resolvedResources.get(dependency.getName());
        ConfigurationFilter filter = resolvedReference.getReferenceFilter();
        String appName = (String) module.getParameters()
                                        .get(SupportedParameters.APP_NAME);
        Resource resource = resolvedReference.getReference();
        Module adaptedModule = getContainingOneRequiresDependency(module, dependency);

        return ConfigurationSubscription.from(mtaId, spaceId, appName, filter, adaptedModule, resource, getMajorSchemaVersion());
    }

    protected Module getContainingOneRequiresDependency(Module module, RequiredDependency dependency) {
        return Module.createV2()
                     .setName(module.getName())
                     .setType(module.getType())
                     .setPath(module.getPath())
                     .setDescription(module.getDescription())
                     .setProperties(module.getProperties())
                     .setParameters(module.getParameters())
                     .setProvidedDependencies(module.getProvidedDependencies())
                     .setRequiredDependencies(Arrays.asList(dependency));
    }

    protected int getMajorSchemaVersion() {
        return MTA_MAJOR_SCHEMA_VERSION;
    }

    private boolean shouldCreateSubscription(RequiredDependency dependency) {
        return (boolean) dependency.getParameters()
                                   .getOrDefault(SupportedParameters.MANAGED, false);
    }

}

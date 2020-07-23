package com.sap.cloud.lm.sl.cf.core.helpers.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.resolvers.v2.DescriptorReferenceResolver;

import com.sap.cloud.lm.sl.cf.core.model.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ResolvedConfigurationReference;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.resolvers.v2.PartialDescriptorReferenceResolver;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;

public class ConfigurationSubscriptionFactory {

    private static final int MTA_MAJOR_SCHEMA_VERSION = 2;

    private DeploymentDescriptor descriptor;
    protected final Map<String, ResolvedConfigurationReference> resolvedResources;

    public ConfigurationSubscriptionFactory(DeploymentDescriptor descriptor,
                                            Map<String, ResolvedConfigurationReference> resolvedResources) {
        this.descriptor = descriptor;
        this.resolvedResources = resolvedResources;
    }

    public List<ConfigurationSubscription> create(String spaceId) {
        List<String> dependenciesToIgnore = new ArrayList<>(resolvedResources.keySet());
        descriptor = getPartialDescriptorReferenceResolver(descriptor, dependenciesToIgnore).resolve();
        return descriptor.getModules()
                         .stream()
                         .flatMap(module -> createSubscriptionsForModule(module, spaceId))
                         .collect(Collectors.toList());
    }

    private Stream<ConfigurationSubscription> createSubscriptionsForModule(Module module, String spaceId) {
        return module.getRequiredDependencies()
                     .stream()
                     .filter(this::shouldCreateSubscription)
                     .map(dependency -> createSubscription(dependency, module, spaceId));
    }

    protected DescriptorReferenceResolver getPartialDescriptorReferenceResolver(DeploymentDescriptor descriptor,
                                                                                List<String> dependenciesToIgnore) {
        return new PartialDescriptorReferenceResolver(descriptor, dependenciesToIgnore);
    }

    private ConfigurationSubscription createSubscription(RequiredDependency dependency, Module module, String spaceId) {
        ResolvedConfigurationReference resolvedReference = resolvedResources.get(dependency.getName());
        ConfigurationFilter filter = resolvedReference.getReferenceFilter();
        String appName = NameUtil.getApplicationName(module);
        Resource resource = resolvedReference.getReference();
        Module adaptedModule = getContainingOneRequiresDependency(module, dependency);
        String mtaId = descriptor.getId();

        return ConfigurationSubscription.from(mtaId, spaceId, appName, filter, adaptedModule, resource, MTA_MAJOR_SCHEMA_VERSION);
    }

    private Module getContainingOneRequiresDependency(Module module, RequiredDependency dependency) {
        return Module.createV2()
                     .setName(module.getName())
                     .setType(module.getType())
                     .setPath(module.getPath())
                     .setDescription(module.getDescription())
                     .setProperties(module.getProperties())
                     .setParameters(module.getParameters())
                     .setProvidedDependencies(module.getProvidedDependencies())
                     .setRequiredDependencies(Collections.singletonList(dependency));
    }

    protected boolean shouldCreateSubscription(RequiredDependency dependency) {
        return (boolean) dependency.getParameters()
                                   .getOrDefault(SupportedParameters.MANAGED, false);
    }

}

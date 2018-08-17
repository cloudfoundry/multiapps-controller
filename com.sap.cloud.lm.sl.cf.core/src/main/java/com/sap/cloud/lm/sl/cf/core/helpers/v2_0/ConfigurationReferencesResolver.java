package com.sap.cloud.lm.sl.cf.core.helpers.v2_0;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.helpers.expander.PropertiesExpander;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ResolvedConfigurationReference;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.mta.model.PropertiesContainer;
import com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2_0.Module;
import com.sap.cloud.lm.sl.mta.model.v2_0.RequiredDependency;

public class ConfigurationReferencesResolver extends com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ConfigurationReferencesResolver {

    protected Map<RequiredDependency, List<RequiredDependency>> expandedDependenciesMap = new HashMap<>();
    private List<String> expandedProperties = new ArrayList<>();

    public ConfigurationReferencesResolver(ConfigurationEntryDao dao, ConfigurationFilterParser filterParser,
        BiFunction<String, String, String> spaceIdSupplier, CloudTarget cloudTarget, ApplicationConfiguration configuration) {
        super(dao, filterParser, spaceIdSupplier, cloudTarget, configuration);
    }

    @Override
    protected void updateReferencesToResolvedResources(com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor descriptor) {
        updateReferencesToResolvedResources((DeploymentDescriptor) descriptor);
    }

    protected void updateReferencesToResolvedResources(DeploymentDescriptor descriptor) {
        for (Module module : descriptor.getModules2_0()) {
            module.setRequiredDependencies2_0(getUpdatedRequiredDependencies(descriptor, module));

            Map<String, Object> properties = module.getProperties();
            for (RequiredDependency originalDependency : expandedDependenciesMap.keySet()) {
                List<RequiredDependency> expandedOriginalDependency = expandedDependenciesMap.get(originalDependency);
                PropertiesExpander expander = new PropertiesExpander(originalDependency.getName(), getNames(expandedOriginalDependency));
                properties = expander.expand(properties);
                expandedProperties.addAll(expander.getExpandedProperties());
            }
            module.setProperties(properties);
        }
    }

    protected List<String> getNames(List<RequiredDependency> dependencies) {
        return dependencies.stream()
            .map(dependency -> dependency.getName())
            .collect(Collectors.toList());
    }

    protected List<RequiredDependency> getUpdatedRequiredDependencies(DeploymentDescriptor descriptor, Module module) {
        List<RequiredDependency> requiredDependencies = new ArrayList<>();
        for (RequiredDependency dependency : module.getRequiredDependencies2_0()) {
            requiredDependencies.addAll(expandRequiredDependencyIfNecessary(descriptor, module, dependency));
        }
        return requiredDependencies;
    }

    protected RequiredDependency createRequiredDependency(com.sap.cloud.lm.sl.mta.model.v1_0.Resource resource,
        RequiredDependency dependency) {
        RequiredDependency.Builder builder = new RequiredDependency.Builder();
        builder.setName(resource.getName());
        builder.setGroup(dependency.getGroup());
        builder.setList(dependency.getList());
        builder.setParameters(dependency.getParameters());
        builder.setProperties(dependency.getProperties());
        return builder.build();
    }

    protected List<RequiredDependency> expandRequiredDependencyIfNecessary(DeploymentDescriptor descriptor,
        PropertiesContainer dependencyOwner, RequiredDependency dependency) {
        ResolvedConfigurationReference resolvedReference = resolvedReferences.get(dependency.getName());

        if (!refersToResolvedResource(dependency)) {
            return Arrays.asList(dependency);
        }

        if (!permitsMultipleResources(dependency)) {
            makeSureIsResolvedToSingleResource(dependency.getName(), resolvedReference.getResolvedResources());
            return Arrays.asList(dependency);
        }

        if (resolvedReference.getResolvedResources()
            .isEmpty()) {
            String listName = dependency.getList();
            dependencyOwner.setProperties(putEmptyListProperty(dependencyOwner.getProperties(), listName));
        }

        List<RequiredDependency> expandedDependencies = resolvedReference.getResolvedResources()
            .stream()
            .map(resource -> createRequiredDependency(resource, dependency))
            .collect(Collectors.toList());
        expandedDependenciesMap.put(dependency, expandedDependencies);
        return expandedDependencies;
    }

    protected boolean permitsMultipleResources(RequiredDependency dependency) {
        return dependency.getList() != null;
    }

    protected boolean refersToResolvedResource(RequiredDependency dependency) {
        return resolvedReferences.containsKey(dependency.getName());
    }

    protected Map<String, Object> putEmptyListProperty(Map<String, Object> properties, String listPropertyName) {
        Map<String, Object> result = new TreeMap<>(properties);
        result.putIfAbsent(listPropertyName, Collections.emptyList());
        return result;
    }

    @Override
    public List<String> getExpandedProperties() {
        return expandedProperties;
    }

    @Override
    protected ConfigurationReferenceResolver createReferenceResolver(ConfigurationEntryDao dao) {
        return new ConfigurationReferenceResolver(dao, configuration);
    }

}

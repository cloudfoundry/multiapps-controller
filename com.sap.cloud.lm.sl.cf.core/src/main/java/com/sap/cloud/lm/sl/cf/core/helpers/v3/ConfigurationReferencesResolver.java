package com.sap.cloud.lm.sl.cf.core.helpers.v3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.core.helpers.expander.PropertiesExpander;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationFilterParser;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.model.ResolvedConfigurationReference;
import com.sap.cloud.lm.sl.cf.core.persistence.service.ConfigurationEntryService;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.ElementContext;
import com.sap.cloud.lm.sl.mta.model.PropertiesContainer;
import com.sap.cloud.lm.sl.mta.model.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.Resource;

public class ConfigurationReferencesResolver extends com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationReferencesResolver {

    private List<Resource> inactiveConfigResources = new ArrayList<>();

    public ConfigurationReferencesResolver(ConfigurationEntryService configurationEntryService, ConfigurationFilterParser filterParser,
                                           CloudTarget cloudTarget, ApplicationConfiguration configuration) {
        super(configurationEntryService, filterParser, cloudTarget, configuration);
    }

    @Override
    protected ConfigurationReferenceResolver createReferenceResolver(ConfigurationEntryService configurationEntryService) {
        return new ConfigurationReferenceResolver(configurationEntryService, configuration);
    }

    @Override
    protected void updateReferencesToResolvedResources(DeploymentDescriptor descriptor) {
        super.updateReferencesToResolvedResources(descriptor);
        for (Resource resource : descriptor.getResources()) {
            // TODO consider subscription support for resources
            resource.setRequiredDependencies(getUpdatedRequiredDependencies(resource));

            Map<String, Object> properties = resource.getProperties();
            Set<RequiredDependency> dependencies = expandedDependenciesMap.keySet();
            for (RequiredDependency dependency : dependencies) {

                List<String> expandedDependenciesNames = getNames(expandedDependenciesMap.get(dependency));
                PropertiesExpander expander = new PropertiesExpander(dependency.getName(), expandedDependenciesNames);
                properties = expander.expand(properties);
            }
            resource.setProperties(properties);
        }
    }

    @Override
    protected List<RequiredDependency> expandRequiredDependencyIfNecessary(PropertiesContainer dependencyOwner,
                                                                           RequiredDependency dependency) {
        ResolvedConfigurationReference resolvedReference = resolvedReferences.get(dependency.getName());

        if (!refersToResolvedResource(dependency)) {
            return Arrays.asList(dependency);
        }

        if (refersToInactiveResource(dependency)) {
            if (permitsMultipleResources(dependency)) {
                setEmptyListProperty(dependencyOwner, dependency);
                expandedDependenciesMap.put(dependency, Collections.emptyList());
            }
            return Collections.emptyList();
        }

        if (!permitsMultipleResources(dependency)) {
            makeSureIsResolvedToSingleResource(dependency.getName(), resolvedReference.getResolvedResources());
            return Arrays.asList(dependency);
        }

        if (resolvedReference.getResolvedResources()
                             .isEmpty()) {
            setEmptyListProperty(dependencyOwner, dependency);
        }

        List<RequiredDependency> expandedDependencies = resolvedReference.getResolvedResources()
                                                                         .stream()
                                                                         .map(resource -> createRequiredDependency(resource, dependency))
                                                                         .collect(Collectors.toList());
        expandedDependenciesMap.put(dependency, expandedDependencies);
        return expandedDependencies;
    }

    private void setEmptyListProperty(PropertiesContainer dependencyOwner, RequiredDependency dependencyV3) {
        String listName = dependencyV3.getList();
        dependencyOwner.setProperties(putEmptyListProperty(dependencyOwner.getProperties(), listName));
    }

    private boolean refersToInactiveResource(RequiredDependency dependency) {
        return inactiveConfigResources.stream()
                                      .anyMatch(resource -> resource.getName()
                                                                    .equals(dependency.getName()));
    }

    protected List<RequiredDependency> getUpdatedRequiredDependencies(Resource resource) {
        return resource.getRequiredDependencies()
                       .stream()
                       .map(dependency -> expandRequiredDependencyIfNecessary(resource, dependency))
                       .flatMap(List::stream)
                       .collect(Collectors.toList());
    }

    @Override
    protected RequiredDependency createRequiredDependency(Resource resource, RequiredDependency dependency) {
        return RequiredDependency.createV3()
                                 .setName(resource.getName())
                                 .setGroup(dependency.getGroup())
                                 .setList(dependency.getList())
                                 .setParameters(dependency.getParameters())
                                 .setProperties(dependency.getProperties())
                                 .setParametersMetadata(dependency.getParametersMetadata())
                                 .setPropertiesMetadata(dependency.getPropertiesMetadata());
    }

    @Override
    public void visit(ElementContext context, Resource sourceResource) {
        ConfigurationFilter configurationFilter = filterParser.parse(sourceResource);
        if (configurationFilter == null) {
            // resource is not a config reference.
            return;
        }
        if (!sourceResource.isActive()) {
            inactiveConfigResources.add(sourceResource);
            // bind empty collection of resources to this config resource in order to replace it with nothing so it is not processed
            ResolvedConfigurationReference resolvedReference = new ResolvedConfigurationReference(configurationFilter,
                                                                                                  sourceResource,
                                                                                                  Collections.emptyList());
            resolvedReferences.put(sourceResource.getName(), resolvedReference);
            return;
        }
        List<Resource> resolvedResources = configurationResolver.resolve(sourceResource, configurationFilter, cloudTarget);
        ResolvedConfigurationReference resolvedReference = new ResolvedConfigurationReference(configurationFilter,
                                                                                              sourceResource,
                                                                                              resolvedResources);
        resolvedReferences.put(sourceResource.getName(), resolvedReference);
    }

}

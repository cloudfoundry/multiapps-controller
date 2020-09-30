package org.cloudfoundry.multiapps.controller.core.helpers.v2;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.helpers.expander.PropertiesExpander;
import org.cloudfoundry.multiapps.controller.core.model.ResolvedConfigurationReference;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.persistence.model.CloudTarget;
import org.cloudfoundry.multiapps.controller.persistence.model.filters.ConfigurationFilter;
import org.cloudfoundry.multiapps.controller.persistence.services.ConfigurationEntryService;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.ElementContext;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.PropertiesContainer;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;
import org.cloudfoundry.multiapps.mta.model.Resource;
import org.cloudfoundry.multiapps.mta.model.Visitor;

public class ConfigurationReferencesResolver extends Visitor {

    protected final ConfigurationReferenceResolver configurationResolver;
    protected final ConfigurationEntryService configurationEntryService;
    protected final Map<String, ResolvedConfigurationReference> resolvedReferences = new TreeMap<>();
    protected final ConfigurationFilterParser filterParser;
    protected final CloudTarget cloudTarget;
    protected final ApplicationConfiguration configuration;

    protected final Map<RequiredDependency, List<RequiredDependency>> expandedDependenciesMap = new HashMap<>();
    private final List<String> expandedProperties = new ArrayList<>();

    public ConfigurationReferencesResolver(ConfigurationEntryService configurationEntryService, ConfigurationFilterParser filterParser,
                                           CloudTarget cloudTarget, ApplicationConfiguration configuration) {
        this.configurationEntryService = configurationEntryService;
        this.filterParser = filterParser;
        this.cloudTarget = cloudTarget;
        this.configuration = configuration;
        this.configurationResolver = createReferenceResolver(configurationEntryService);
    }

    public void resolve(DeploymentDescriptor descriptor) {
        descriptor.accept(this);
        insertResolvedResources(descriptor);
    }

    protected void insertResolvedResources(DeploymentDescriptor descriptor) {
        descriptor.setResources(getResolvedResources(descriptor));
        updateReferencesToResolvedResources(descriptor);
    }

    protected List<Resource> getResolvedResources(DeploymentDescriptor descriptor) {
        return descriptor.getResources()
                         .stream()
                         .map(this::getResolvedResources)
                         .flatMap(List::stream)
                         .collect(Collectors.toList());
    }

    protected List<Resource> getResolvedResources(Resource resource) {
        ResolvedConfigurationReference reference = resolvedReferences.get(resource.getName());
        if (reference != null) {
            return reference.getResolvedResources();
        }
        return Collections.singletonList(resource);
    }

    protected void updateReferencesToResolvedResources(DeploymentDescriptor descriptor) {
        for (Module module : descriptor.getModules()) {
            module.setRequiredDependencies(getUpdatedRequiredDependencies(module));

            Map<String, Object> properties = module.getProperties();
            for (Map.Entry<RequiredDependency, List<RequiredDependency>> entry : expandedDependenciesMap.entrySet()) {
                RequiredDependency originalDependency = entry.getKey();
                List<RequiredDependency> expandedOriginalDependency = entry.getValue();
                PropertiesExpander expander = new PropertiesExpander(originalDependency.getName(), getNames(expandedOriginalDependency));
                properties = expander.expand(properties);
                expandedProperties.addAll(expander.getExpandedProperties());
            }
            module.setProperties(properties);
        }
    }

    protected List<String> getNames(List<RequiredDependency> dependencies) {
        return dependencies.stream()
                           .map(RequiredDependency::getName)
                           .collect(Collectors.toList());
    }

    protected List<RequiredDependency> getUpdatedRequiredDependencies(Module module) {
        return module.getRequiredDependencies()
                     .stream()
                     .map(dependency -> expandRequiredDependencyIfNecessary(module, dependency))
                     .flatMap(List::stream)
                     .collect(Collectors.toList());
    }

    protected RequiredDependency createRequiredDependency(Resource resource, RequiredDependency dependency) {
        return RequiredDependency.createV2()
                                 .setName(resource.getName())
                                 .setGroup(dependency.getGroup())
                                 .setList(dependency.getList())
                                 .setParameters(dependency.getParameters())
                                 .setProperties(dependency.getProperties());
    }

    protected List<RequiredDependency> expandRequiredDependencyIfNecessary(PropertiesContainer dependencyOwner,
                                                                           RequiredDependency dependency) {
        ResolvedConfigurationReference resolvedReference = resolvedReferences.get(dependency.getName());

        if (!refersToResolvedResource(dependency)) {
            return Collections.singletonList(dependency);
        }

        if (!permitsMultipleResources(dependency)) {
            makeSureIsResolvedToSingleResource(dependency.getName(), resolvedReference.getResolvedResources());
            return Collections.singletonList(dependency);
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

    protected void makeSureIsResolvedToSingleResource(String resolvedResourceName, List<Resource> resultingResources) {
        if (resultingResources.size() > 1) {
            throw new ContentException(format(Messages.MULTIPLE_CONFIGURATION_ENTRIES_WERE_FOUND, resolvedResourceName));
        } else if (resultingResources.isEmpty()) {
            throw new ContentException(format(Messages.NO_CONFIGURATION_ENTRIES_WERE_FOUND, resolvedResourceName));
        }
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

    public List<String> getExpandedProperties() {
        return expandedProperties;
    }

    public Map<String, ResolvedConfigurationReference> getResolvedReferences() {
        return resolvedReferences;
    }

    protected ConfigurationReferenceResolver createReferenceResolver(ConfigurationEntryService configurationEntryService) {
        return new ConfigurationReferenceResolver(configurationEntryService, configuration);
    }

    @Override
    public void visit(ElementContext context, Resource sourceResource) {
        ConfigurationFilter configurationFilter = filterParser.parse(sourceResource);
        if (configurationFilter == null) {
            // resource is not a config reference.
            return;
        }
        List<Resource> resolvedResources = configurationResolver.resolve(sourceResource, configurationFilter, cloudTarget);
        ResolvedConfigurationReference resolvedReference = new ResolvedConfigurationReference(configurationFilter,
                                                                                              sourceResource,
                                                                                              resolvedResources);
        resolvedReferences.put(sourceResource.getName(), resolvedReference);
    }
}

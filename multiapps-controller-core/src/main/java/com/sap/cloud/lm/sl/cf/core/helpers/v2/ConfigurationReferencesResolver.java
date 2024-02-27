package com.sap.cloud.lm.sl.cf.core.helpers.v2;

import static java.text.MessageFormat.format;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.helpers.expander.PropertiesExpander;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ResolvedConfigurationReference;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.ElementContext;
import com.sap.cloud.lm.sl.mta.model.PropertiesContainer;
import com.sap.cloud.lm.sl.mta.model.Visitor;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Module;
import com.sap.cloud.lm.sl.mta.model.v2.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;

public class ConfigurationReferencesResolver extends Visitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationReferencesResolver.class);

    protected ConfigurationReferenceResolver configurationResolver;
    protected ConfigurationEntryDao dao;
    protected Map<String, ResolvedConfigurationReference> resolvedReferences = new TreeMap<>();
    protected ConfigurationFilterParser filterParser;
    protected BiFunction<String, String, String> spaceIdSupplier;
    protected CloudTarget cloudTarget;
    protected ApplicationConfiguration configuration;

    protected Map<RequiredDependency, List<RequiredDependency>> expandedDependenciesMap = new HashMap<>();
    private List<String> expandedProperties = new ArrayList<>();

    public ConfigurationReferencesResolver(ConfigurationEntryDao dao, ConfigurationFilterParser filterParser,
                                           BiFunction<String, String, String> spaceIdSupplier, CloudTarget cloudTarget,
                                           ApplicationConfiguration configuration) {
        this.dao = dao;
        this.filterParser = filterParser;
        this.spaceIdSupplier = spaceIdSupplier;
        this.cloudTarget = cloudTarget;
        this.configuration = configuration;
        this.configurationResolver = createReferenceResolver(dao);
    }

    public void resolve(DeploymentDescriptor descriptor) {
        descriptor.accept(this);
        insertResolvedResources(descriptor);
    }

    protected void insertResolvedResources(DeploymentDescriptor descriptor) {
        descriptor.setResources2(getResolvedResources(descriptor));
        updateReferencesToResolvedResources(descriptor);
    }

    protected List<Resource> getResolvedResources(DeploymentDescriptor descriptor) {
        return descriptor.getResources2()
                         .stream()
                         .flatMap(resource -> getResolvedResources(resource).stream())
                         .collect(Collectors.toList());
    }

    protected List<Resource> getResolvedResources(Resource resource) {
        ResolvedConfigurationReference reference = resolvedReferences.get(resource.getName());
        if (reference != null) {
            return reference.getResolvedResources();
        }
        return Arrays.asList(resource);
    }

    protected void updateReferencesToResolvedResources(DeploymentDescriptor descriptor) {
        for (Module module : descriptor.getModules2()) {
            module.setRequiredDependencies2(getUpdatedRequiredDependencies(descriptor, module));

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
                           .map(RequiredDependency::getName)
                           .collect(Collectors.toList());
    }

    protected List<RequiredDependency> getUpdatedRequiredDependencies(DeploymentDescriptor descriptor, Module module) {
        List<RequiredDependency> requiredDependencies = new ArrayList<>();
        for (RequiredDependency dependency : module.getRequiredDependencies2()) {
            requiredDependencies.addAll(expandRequiredDependencyIfNecessary(descriptor, module, dependency));
        }
        return requiredDependencies;
    }

    protected RequiredDependency createRequiredDependency(Resource resource, RequiredDependency dependency) {
        RequiredDependency.Builder builder = new RequiredDependency.Builder();
        builder.setName(resource.getName());
        builder.setGroup(dependency.getGroup());
        builder.setList(dependency.getList());
        builder.setParameters(dependency.getParameters());
        builder.setProperties(dependency.getProperties());
        return builder.build();
    }

    protected List<RequiredDependency> expandRequiredDependencyIfNecessary(DeploymentDescriptor descriptor,
                                                                           PropertiesContainer dependencyOwner,
                                                                           RequiredDependency dependency) {
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

    protected void makeSureIsResolvedToSingleResource(String resolvedResourceName, List<Resource> resultingResources) {
        if (resultingResources.size() > 1) {
            LOGGER.debug(Messages.MULTIPLE_CONFIGURATION_ENTRIES, resolvedResourceName, resultingResources);
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

    protected ConfigurationReferenceResolver createReferenceResolver(ConfigurationEntryDao dao) {
        return new ConfigurationReferenceResolver(dao, configuration);
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

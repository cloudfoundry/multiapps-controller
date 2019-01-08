package com.sap.cloud.lm.sl.cf.core.helpers.v3;

import static com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil.isActive;
import static com.sap.cloud.lm.sl.common.util.CommonUtil.cast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.dao.filters.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.helpers.expander.PropertiesExpander;
import com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationFilterParser;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ResolvedConfigurationReference;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.mta.model.ElementContext;
import com.sap.cloud.lm.sl.mta.model.PropertiesContainer;
import com.sap.cloud.lm.sl.mta.model.v3.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.v3.Resource;

public class ConfigurationReferencesResolver extends com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationReferencesResolver {

    private List<Resource> inactiveConfigResources = new ArrayList<>();

    public ConfigurationReferencesResolver(ConfigurationEntryDao dao, ConfigurationFilterParser filterParser,
        BiFunction<String, String, String> spaceIdSupplier, CloudTarget cloudTarget, ApplicationConfiguration configuration) {
        super(dao, filterParser, spaceIdSupplier, cloudTarget, configuration);
    }

    @Override
    protected ConfigurationReferenceResolver createReferenceResolver(ConfigurationEntryDao dao) {
        return new ConfigurationReferenceResolver(dao, configuration);
    }

    @Override
    protected void updateReferencesToResolvedResources(com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor descriptor) {
        DeploymentDescriptor descriptor3 = (DeploymentDescriptor) descriptor;
        this.updateReferencesToResolvedResources(descriptor3);
    }

    protected void updateReferencesToResolvedResources(DeploymentDescriptor descriptor) {
        super.updateReferencesToResolvedResources(descriptor);
        com.sap.cloud.lm.sl.mta.model.v3.DeploymentDescriptor descriptor3 = cast(descriptor);
        for (Resource resource : descriptor3.getResources3()) {
            // TODO consider subscription support for resources
            resource.setRequiredDependencies3(getUpdatedRequiredDependencies(descriptor, resource));

            Map<String, Object> properties = resource.getProperties();
            Set<RequiredDependency> dependencies = cast(expandedDependenciesMap.keySet());
            for (RequiredDependency dependency : dependencies) {

                List<String> expandedDependenciesNames = getNames(expandedDependenciesMap.get(dependency));
                PropertiesExpander expander = new PropertiesExpander(dependency.getName(), expandedDependenciesNames);
                properties = expander.expand(properties);
            }
            resource.setProperties(properties);
        }
    }

    @Override
    protected List<com.sap.cloud.lm.sl.mta.model.v2.RequiredDependency> expandRequiredDependencyIfNecessary(
        com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor descriptor, PropertiesContainer dependencyOwner,
        com.sap.cloud.lm.sl.mta.model.v2.RequiredDependency dependency) {
        RequiredDependency dependencyV3 = (RequiredDependency) dependency;
        ResolvedConfigurationReference resolvedReference = resolvedReferences.get(dependencyV3.getName());

        if (!refersToResolvedResource(dependencyV3)) {
            return Arrays.asList(dependencyV3);
        }

        if (refersToInactiveResource(dependencyV3)) {
            if (permitsMultipleResources(dependencyV3)) {
                setEmptyListProperty(dependencyOwner, dependencyV3);
                expandedDependenciesMap.put(dependencyV3, Collections.emptyList());
            }
            return Collections.emptyList();
        }

        if (!permitsMultipleResources(dependencyV3)) {
            makeSureIsResolvedToSingleResource(dependencyV3.getName(), resolvedReference.getResolvedResources());
            return Arrays.asList(dependencyV3);
        }

        if (resolvedReference.getResolvedResources()
            .isEmpty()) {
            setEmptyListProperty(dependencyOwner, dependencyV3);
        }

        List<com.sap.cloud.lm.sl.mta.model.v2.RequiredDependency> expandedDependencies = resolvedReference.getResolvedResources()
            .stream()
            .map(resource -> createRequiredDependency(resource, dependencyV3))
            .collect(Collectors.toList());
        expandedDependenciesMap.put(dependencyV3, expandedDependencies);
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

    protected List<RequiredDependency> getUpdatedRequiredDependencies(DeploymentDescriptor descriptor, Resource resource) {
        List<RequiredDependency> requiredDependencies = new ArrayList<>();
        for (RequiredDependency dependency : resource.getRequiredDependencies3()) {
            List<RequiredDependency> dependencies = cast(expandRequiredDependencyIfNecessary(descriptor, resource, dependency));
            requiredDependencies.addAll(dependencies);
        }
        return requiredDependencies;
    }

    @Override
    protected RequiredDependency createRequiredDependency(com.sap.cloud.lm.sl.mta.model.v2.Resource resource,
        com.sap.cloud.lm.sl.mta.model.v2.RequiredDependency dependency) {
        RequiredDependency.Builder builder = new RequiredDependency.Builder();
        builder.setName(resource.getName());
        RequiredDependency dependency3 = cast(dependency);
        builder.setGroup(dependency3.getGroup());
        builder.setList(dependency3.getList());
        builder.setParameters(dependency3.getParameters());
        builder.setProperties(dependency3.getProperties());
        builder.setParametersMetadata(dependency3.getParametersMetadata());
        builder.setPropertiesMetadata(dependency3.getPropertiesMetadata());
        return builder.build();
    }

    @Override
    public void visit(ElementContext context, com.sap.cloud.lm.sl.mta.model.v2.Resource sourceResource) {
        ConfigurationFilter configurationFilter = filterParser.parse(sourceResource);
        if (configurationFilter == null) {
            // resource is not a config reference.
            return;
        }
        if (!isActive(sourceResource)) {
            inactiveConfigResources.add((Resource) sourceResource);
            // bind empty collection of resources to this config resource in order to replace it with nothing so it is not processed
            ResolvedConfigurationReference resolvedReference = new ResolvedConfigurationReference(configurationFilter, sourceResource,
                Collections.emptyList());
            resolvedReferences.put(sourceResource.getName(), resolvedReference);
            return;
        }
        List<com.sap.cloud.lm.sl.mta.model.v2.Resource> resolvedResources = configurationResolver.resolve(sourceResource,
            configurationFilter, cloudTarget);
        ResolvedConfigurationReference resolvedReference = new ResolvedConfigurationReference(configurationFilter, sourceResource,
            resolvedResources);
        resolvedReferences.put(sourceResource.getName(), resolvedReference);
    }

}

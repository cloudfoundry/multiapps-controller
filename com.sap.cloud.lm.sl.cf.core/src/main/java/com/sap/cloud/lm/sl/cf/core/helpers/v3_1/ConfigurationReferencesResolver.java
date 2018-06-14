package com.sap.cloud.lm.sl.cf.core.helpers.v3_1;

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
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ConfigurationFilterParser;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.model.ResolvedConfigurationReference;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.ElementContext;
import com.sap.cloud.lm.sl.mta.model.PropertiesContainer;
import com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3_1.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.v3_1.Resource;

public class ConfigurationReferencesResolver extends com.sap.cloud.lm.sl.cf.core.helpers.v3_0.ConfigurationReferencesResolver {

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
    protected void updateReferencesToResolvedResources(com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor descriptor)
        throws ContentException {
        DeploymentDescriptor descriptor3_1 = (DeploymentDescriptor) descriptor;
        this.updateReferencesToResolvedResources(descriptor3_1);
    }

    protected void updateReferencesToResolvedResources(DeploymentDescriptor descriptor) throws ContentException {
        super.updateReferencesToResolvedResources(descriptor);
        com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor descriptor3_1 = cast(descriptor);
        for (Resource resource : descriptor3_1.getResources3_1()) {
            // TODO consider subscription support for resources
            resource.setRequiredDependencies3_1(getUpdatedRequiredDependencies(descriptor, resource));

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
    protected List<com.sap.cloud.lm.sl.mta.model.v2_0.RequiredDependency> expandRequiredDependencyIfNecessary(
        com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor descriptor, PropertiesContainer dependencyOwner,
        com.sap.cloud.lm.sl.mta.model.v2_0.RequiredDependency dependency) throws ContentException {
        RequiredDependency dependencyV3 = (RequiredDependency) dependency;
        ResolvedConfigurationReference resolvedReference = resolvedReferences.get(dependencyV3.getName());

        if (!refersToResolvedResource(dependencyV3)) {
            return Arrays.asList(dependencyV3);
        }
        
        if(refersToInactiveResource(dependencyV3)) {
            if(permitsMultipleResources(dependencyV3)) {
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

        List<com.sap.cloud.lm.sl.mta.model.v2_0.RequiredDependency> expandedDependencies = resolvedReference.getResolvedResources()
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

    protected List<RequiredDependency> getUpdatedRequiredDependencies(DeploymentDescriptor descriptor, Resource resource)
        throws ContentException {
        List<RequiredDependency> requiredDependencies = new ArrayList<>();
        for (RequiredDependency dependency : resource.getRequiredDependencies3_1()) {
            List<RequiredDependency> dependencies = cast(expandRequiredDependencyIfNecessary(descriptor, resource, dependency));
            requiredDependencies.addAll(dependencies);
        }
        return requiredDependencies;
    }

    @Override
    protected RequiredDependency createRequiredDependency(com.sap.cloud.lm.sl.mta.model.v1_0.Resource resource,
        com.sap.cloud.lm.sl.mta.model.v2_0.RequiredDependency dependency) {
        RequiredDependency.Builder builder = new RequiredDependency.Builder();
        builder.setName(resource.getName());
        RequiredDependency dependency3_1 = cast(dependency);
        builder.setGroup(dependency3_1.getGroup());
        builder.setList(dependency3_1.getList());
        builder.setParameters(dependency3_1.getParameters());
        builder.setProperties(dependency3_1.getProperties());
        builder.setParametersMetadata(dependency3_1.getParametersMetadata());
        builder.setPropertiesMetadata(dependency3_1.getPropertiesMetadata());
        return builder.build();
    }

    @Override
    public void visit(ElementContext context, com.sap.cloud.lm.sl.mta.model.v1_0.Resource sourceResource) throws ContentException {
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
        List<com.sap.cloud.lm.sl.mta.model.v1_0.Resource> resolvedResources = configurationResolver.resolve(sourceResource,
            configurationFilter, cloudTarget);
        ResolvedConfigurationReference resolvedReference = new ResolvedConfigurationReference(configurationFilter, sourceResource,
            resolvedResources);
        resolvedReferences.put(sourceResource.getName(), resolvedReference);
    }

}

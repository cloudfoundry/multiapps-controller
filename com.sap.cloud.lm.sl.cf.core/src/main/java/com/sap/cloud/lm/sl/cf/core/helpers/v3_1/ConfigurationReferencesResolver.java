package com.sap.cloud.lm.sl.cf.core.helpers.v3_1;

import static com.sap.cloud.lm.sl.common.util.CommonUtil.cast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import com.sap.cloud.lm.sl.cf.core.dao.ConfigurationEntryDao;
import com.sap.cloud.lm.sl.cf.core.helpers.expander.PropertiesExpander;
import com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ConfigurationFilterParser;
import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v3_1.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.v3_1.RequiredDependency.RequiredDependencyBuilder;
import com.sap.cloud.lm.sl.mta.model.v3_1.Resource;

public class ConfigurationReferencesResolver extends com.sap.cloud.lm.sl.cf.core.helpers.v3_0.ConfigurationReferencesResolver {

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
        RequiredDependencyBuilder builder = new RequiredDependencyBuilder();
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

}

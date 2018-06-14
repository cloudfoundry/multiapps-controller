package com.sap.cloud.lm.sl.cf.core.helpers.v3_1;


import static com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil.isActive;

import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.dao.filters.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ResolvedConfigurationReference;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.resolvers.v3_1.PartialDescriptorReferenceResolver;
import com.sap.cloud.lm.sl.mta.model.v2_0.Module;
import com.sap.cloud.lm.sl.mta.model.v2_0.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.v3_1.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.resolvers.v2_0.DescriptorReferenceResolver;

public class ConfigurationSubscriptionFactory extends com.sap.cloud.lm.sl.cf.core.helpers.v2_0.ConfigurationSubscriptionFactory {

    @Override
    protected DescriptorReferenceResolver getPartialDescriptorReferenceResolver(
        com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor descriptor, List<String> dependenciesToIgnore) {
        return new PartialDescriptorReferenceResolver((DeploymentDescriptor) descriptor, dependenciesToIgnore);
    }

    @Override
    protected ConfigurationSubscription createSubscription(String spaceId, String mtaId, Module module, RequiredDependency dependency,
        Map<String, ResolvedConfigurationReference> resolvedResources) {
        ResolvedConfigurationReference resolvedReference = resolvedResources.get(dependency.getName());
        if(!isActive(resolvedReference.getReference())) {
            return null;
        }
        ConfigurationFilter filter = resolvedReference.getReferenceFilter();
        String appName = (String) module.getParameters()
            .get(SupportedParameters.APP_NAME);
        com.sap.cloud.lm.sl.mta.model.v1_0.Resource resource = resolvedReference.getReference();
        Module adaptedModule = getContainingOneRequiresDependency(module, dependency);

        return ConfigurationSubscription.from(mtaId, spaceId, appName, filter, adaptedModule, resource, getMajorSchemaVersion());
    }
}

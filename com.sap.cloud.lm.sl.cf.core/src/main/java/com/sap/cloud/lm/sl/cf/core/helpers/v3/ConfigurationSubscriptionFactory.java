package com.sap.cloud.lm.sl.cf.core.helpers.v3;

import static com.sap.cloud.lm.sl.cf.core.util.CloudModelBuilderUtil.isActive;

import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.dao.filters.ConfigurationFilter;
import com.sap.cloud.lm.sl.cf.core.model.ConfigurationSubscription;
import com.sap.cloud.lm.sl.cf.core.model.ResolvedConfigurationReference;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.resolvers.v3.PartialDescriptorReferenceResolver;
import com.sap.cloud.lm.sl.mta.model.v2.Module;
import com.sap.cloud.lm.sl.mta.model.v2.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.v3.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.resolvers.v2.DescriptorReferenceResolver;

public class ConfigurationSubscriptionFactory extends com.sap.cloud.lm.sl.cf.core.helpers.v2.ConfigurationSubscriptionFactory {

    @Override
    protected DescriptorReferenceResolver getPartialDescriptorReferenceResolver(
        com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor descriptor, List<String> dependenciesToIgnore) {
        return new PartialDescriptorReferenceResolver((DeploymentDescriptor) descriptor, dependenciesToIgnore);
    }

    @Override
    protected ConfigurationSubscription createSubscription(String spaceId, String mtaId, Module module, RequiredDependency dependency,
        Map<String, ResolvedConfigurationReference> resolvedResources) {
        ResolvedConfigurationReference resolvedReference = resolvedResources.get(dependency.getName());
        if (!isActive(resolvedReference.getReference())) {
            return null;
        }
        ConfigurationFilter filter = resolvedReference.getReferenceFilter();
        String appName = (String) module.getParameters()
            .get(SupportedParameters.APP_NAME);
        com.sap.cloud.lm.sl.mta.model.v2.Resource resource = resolvedReference.getReference();
        Module adaptedModule = getContainingOneRequiresDependency(module, dependency);

        return ConfigurationSubscription.from(mtaId, spaceId, appName, filter, adaptedModule, resource, getMajorSchemaVersion());
    }
}

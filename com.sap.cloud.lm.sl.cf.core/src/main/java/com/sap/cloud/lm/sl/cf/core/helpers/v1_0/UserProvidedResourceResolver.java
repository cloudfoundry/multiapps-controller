package com.sap.cloud.lm.sl.cf.core.helpers.v1_0;

import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertyValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.mta.builders.v1_0.PropertiesChainBuilder;
import com.sap.cloud.lm.sl.mta.model.v1_0.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Module;
import com.sap.cloud.lm.sl.mta.model.v1_0.Resource;
import com.sap.cloud.lm.sl.mta.model.v1_0.Resource.ResourceBuilder;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatform;
import com.sap.cloud.lm.sl.mta.model.v1_0.TargetPlatformType;

public class UserProvidedResourceResolver {

    protected ResourceTypeFinder resourceHelper;
    protected DeploymentDescriptor descriptor;
    private PropertiesChainBuilder propertiesChainBuilder;

    public UserProvidedResourceResolver(ResourceTypeFinder resourceHelper, DeploymentDescriptor descriptor, TargetPlatform platform,
        TargetPlatformType platformType) {
        this.resourceHelper = resourceHelper;
        this.descriptor = descriptor;
        this.propertiesChainBuilder = new PropertiesChainBuilder(descriptor, platform, platformType);
    }

    public DeploymentDescriptor resolve() {
        List<Resource> descriptorResources = new ArrayList<>(descriptor.getResources1_0());
        for (Module module : descriptor.getModules1_0()) {
            Resource userProvidedResource = getResource(buildModuleChain(module));
            if (userProvidedResource != null) {
                updateModuleRequiredDependencies(module, userProvidedResource);
                descriptorResources.add(userProvidedResource);
            }
        }
        descriptor.setResources1_0(descriptorResources);
        return descriptor;
    }

    protected void updateModuleRequiredDependencies(Module module, Resource userProvidedResource) {
        List<String> moduleRequiredDependencies = new ArrayList<>(module.getRequiredDependencies1_0());
        moduleRequiredDependencies.add(userProvidedResource.getName());
        module.setRequiredDependencies1_0(moduleRequiredDependencies);
    }

    protected List<Map<String, Object>> buildModuleChain(Module module) {
        return propertiesChainBuilder.buildModuleChain(module.getName());
    }

    @SuppressWarnings("unchecked")
    protected Resource getResource(List<Map<String, Object>> parametersList) {
        if (!shouldCreateUserProvidedService(parametersList)) {
            return null;
        }
        String userProvidedServiceName = (String) getPropertyValue(parametersList, SupportedParameters.USER_PROVIDED_SERVICE_NAME, null);
        if (userProvidedServiceName == null || userProvidedServiceName.isEmpty()) {
            return null;
        }
        Map<String, Object> userProvidedServiceConfig = (Map<String, Object>) getPropertyValue(parametersList,
            SupportedParameters.USER_PROVIDED_SERVICE_CONFIG, Collections.emptyMap());

        return createResource(userProvidedServiceName, MapUtil.asMap(SupportedParameters.SERVICE_CONFIG, userProvidedServiceConfig));
    }

    private boolean shouldCreateUserProvidedService(List<Map<String, Object>> parametersList) {
        return (Boolean) getPropertyValue(parametersList, SupportedParameters.CREATE_USER_PROVIDED_SERVICE, false);
    }

    protected Resource createResource(String userProvidedServiceName, Map<String, Object> parameters) {
        ResourceBuilder builder = getResourceBuilder();
        builder.setName(userProvidedServiceName);
        builder.setType(resourceHelper.getResourceTypeName());
        builder.setProperties(parameters);
        return builder.build();
    }

    protected ResourceBuilder getResourceBuilder() {
        return new ResourceBuilder();
    }

}

package com.sap.cloud.lm.sl.cf.core.helpers.v2;

import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertyValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.mta.builders.v2.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.model.Platform;
import com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.v2.Module;
import com.sap.cloud.lm.sl.mta.model.v2.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;

public class UserProvidedResourceResolver {

    protected ResourceTypeFinder resourceHelper;
    protected DeploymentDescriptor descriptor;
    private ParametersChainBuilder parametersChainBuilder;

    public UserProvidedResourceResolver(ResourceTypeFinder resourceHelper, DeploymentDescriptor descriptor,
        Platform platform) {
        this.resourceHelper = resourceHelper;
        this.descriptor = descriptor;
        this.parametersChainBuilder = new ParametersChainBuilder(descriptor, platform);
    }

    public DeploymentDescriptor resolve() {
        List<Resource> descriptorResources = new ArrayList<>(descriptor.getResources2());
        for (Module module : descriptor.getModules2()) {
            Resource userProvidedResource = getResource(buildModuleChain(module));
            if (userProvidedResource != null) {
                updateModuleRequiredDependencies(module, userProvidedResource);
                descriptorResources.add(userProvidedResource);
            }
        }
        descriptor.setResources2(descriptorResources);
        return descriptor;
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

    protected List<Map<String, Object>> buildModuleChain(Module module) {
        return parametersChainBuilder.buildModuleChain(module.getName());
    }

    protected void updateModuleRequiredDependencies(Module module, Resource userProvidedResource) {
        com.sap.cloud.lm.sl.mta.model.v2.Module moduleV2 = module;
        Resource resourceV2 = userProvidedResource;
        List<RequiredDependency> moduleRequiredDependencies = new ArrayList<>(moduleV2.getRequiredDependencies2());
        RequiredDependency.Builder requiredDependencyBuilder = new RequiredDependency.Builder();
        requiredDependencyBuilder.setName(resourceV2.getName());
        moduleRequiredDependencies.add(requiredDependencyBuilder.build());
        moduleV2.setRequiredDependencies2(moduleRequiredDependencies);
    }

    protected Resource createResource(String userProvidedServiceName, Map<String, Object> parameters) {
        Resource.Builder builder = getResourceBuilder();
        builder.setName(userProvidedServiceName);
        builder.setType(resourceHelper.getResourceTypeName());
        builder.setParameters(parameters);
        return builder.build();
    }

    protected Resource.Builder getResourceBuilder() {
        return new Resource.Builder();
    }

}

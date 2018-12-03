package com.sap.cloud.lm.sl.cf.core.helpers.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.helpers.v1.ResourceTypeFinder;
import com.sap.cloud.lm.sl.mta.builders.v2.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.model.v1.Module;
import com.sap.cloud.lm.sl.mta.model.v2.Platform;
import com.sap.cloud.lm.sl.mta.model.v2.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;

public class UserProvidedResourceResolver extends com.sap.cloud.lm.sl.cf.core.helpers.v1.UserProvidedResourceResolver {

    private ParametersChainBuilder parametersChainBuilder;

    public UserProvidedResourceResolver(ResourceTypeFinder resourceHelper,
        com.sap.cloud.lm.sl.mta.model.v2.DeploymentDescriptor descriptor, Platform platform) {
        super(resourceHelper, descriptor, platform);
        this.parametersChainBuilder = new ParametersChainBuilder(descriptor, platform);
    }

    @Override
    protected List<Map<String, Object>> buildModuleChain(com.sap.cloud.lm.sl.mta.model.v1.Module module) {
        return parametersChainBuilder.buildModuleChain(module.getName());
    }

    @Override
    protected void updateModuleRequiredDependencies(Module module, com.sap.cloud.lm.sl.mta.model.v1.Resource userProvidedResource) {
        com.sap.cloud.lm.sl.mta.model.v2.Module moduleV2 = (com.sap.cloud.lm.sl.mta.model.v2.Module) module;
        Resource resourceV2 = (Resource) userProvidedResource;
        List<RequiredDependency> moduleRequiredDependencies = new ArrayList<>(moduleV2.getRequiredDependencies2());
        RequiredDependency.Builder requiredDependencyBuilder = new RequiredDependency.Builder();
        requiredDependencyBuilder.setName(resourceV2.getName());
        moduleRequiredDependencies.add(requiredDependencyBuilder.build());
        moduleV2.setRequiredDependencies2(moduleRequiredDependencies);
    }

    @Override
    protected Resource createResource(String userProvidedServiceName, Map<String, Object> parameters) {
        Resource.Builder builder = getResourceBuilder();
        builder.setName(userProvidedServiceName);
        builder.setType(resourceHelper.getResourceTypeName());
        builder.setParameters(parameters);
        return builder.build();
    }

    @Override
    protected Resource.Builder getResourceBuilder() {
        return new Resource.Builder();
    }

}

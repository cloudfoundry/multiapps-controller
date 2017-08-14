package com.sap.cloud.lm.sl.cf.core.helpers.v2_0;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ResourceTypeFinder;
import com.sap.cloud.lm.sl.mta.builders.v2_0.ParametersChainBuilder;
import com.sap.cloud.lm.sl.mta.model.v1_0.Module;
import com.sap.cloud.lm.sl.mta.model.v2_0.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.v2_0.Resource;
import com.sap.cloud.lm.sl.mta.model.v2_0.Resource.ResourceBuilder;
import com.sap.cloud.lm.sl.mta.model.v2_0.Target;
import com.sap.cloud.lm.sl.mta.model.v2_0.Platform;

public class UserProvidedResourceResolver extends com.sap.cloud.lm.sl.cf.core.helpers.v1_0.UserProvidedResourceResolver {

    private ParametersChainBuilder parametersChainBuilder;

    public UserProvidedResourceResolver(ResourceTypeFinder resourceHelper,
        com.sap.cloud.lm.sl.mta.model.v2_0.DeploymentDescriptor descriptor, Target target, Platform platform) {
        super(resourceHelper, descriptor, target, platform);
        this.parametersChainBuilder = new ParametersChainBuilder(descriptor, target, platform);
    }

    @Override
    protected List<Map<String, Object>> buildModuleChain(com.sap.cloud.lm.sl.mta.model.v1_0.Module module) {
        return parametersChainBuilder.buildModuleChain(module.getName());
    }

    @Override
    protected void updateModuleRequiredDependencies(Module module, com.sap.cloud.lm.sl.mta.model.v1_0.Resource userProvidedResource) {
        com.sap.cloud.lm.sl.mta.model.v2_0.Module moduleV2 = (com.sap.cloud.lm.sl.mta.model.v2_0.Module) module;
        Resource resourceV2 = (Resource) userProvidedResource;
        List<RequiredDependency> moduleRequiredDependencies = new ArrayList<>(moduleV2.getRequiredDependencies2_0());
        RequiredDependency.RequiredDependencyBuilder requiredDependencyBuilder = new RequiredDependency.RequiredDependencyBuilder();
        requiredDependencyBuilder.setName(resourceV2.getName());
        moduleRequiredDependencies.add(requiredDependencyBuilder.build());
        moduleV2.setRequiredDependencies2_0(moduleRequiredDependencies);
    }

    @Override
    protected Resource createResource(String userProvidedServiceName, Map<String, Object> parameters) {
        ResourceBuilder builder = getResourceBuilder();
        builder.setName(userProvidedServiceName);
        builder.setType(resourceHelper.getResourceTypeName());
        builder.setParameters(parameters);
        return builder.build();
    }

    @Override
    protected ResourceBuilder getResourceBuilder() {
        return new ResourceBuilder();
    }

}

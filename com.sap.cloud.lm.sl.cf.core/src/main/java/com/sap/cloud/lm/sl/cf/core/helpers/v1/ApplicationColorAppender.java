package com.sap.cloud.lm.sl.cf.core.helpers.v1;

import java.util.Map;
import java.util.TreeMap;

import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.mta.model.ElementContext;
import com.sap.cloud.lm.sl.mta.model.Visitor;
import com.sap.cloud.lm.sl.mta.model.v1.Module;
import com.sap.cloud.lm.sl.mta.model.v1.Resource;

public class ApplicationColorAppender extends Visitor {

    protected static final String RESOURCE_IS_NOT_FOUND_ERROR = "Resource \"{0}\" is not found";

    protected ApplicationColor applicationColor;
    protected ApplicationColor deployedMtaColor;

    public ApplicationColorAppender(ApplicationColor deployedMtaColor, ApplicationColor applicationColor) {
        this.applicationColor = applicationColor;
        this.deployedMtaColor = deployedMtaColor;
    }

    @Override
    public void visit(ElementContext context, Module module) {
        Map<String, Object> properties = new TreeMap<>(module.getProperties());
        properties.put(SupportedParameters.APP_NAME, computeAppName(module));
        module.setProperties(properties);
    }

    protected String computeAppName(Module module) {
        Map<String, Object> moduleProperties = module.getProperties();
        String moduleName = module.getName();
        return this.getAppName(moduleProperties, moduleName);
    }

    protected String getAppName(Map<String, Object> moduleProperties, String moduleName) {
        return (String) moduleProperties.getOrDefault(SupportedParameters.APP_NAME, moduleName) + applicationColor.asSuffix();
    }

    protected Resource buildResource(String resourceName, String resourceType) {
        Resource.Builder builder = getResourceBuilder();
        builder.setName(resourceName);
        builder.setType(resourceType);
        return builder.build();
    }

    protected Resource.Builder getResourceBuilder() {
        return new Resource.Builder();
    }

}

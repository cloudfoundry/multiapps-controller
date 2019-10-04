package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Map;
import java.util.TreeMap;

import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.mta.model.ElementContext;
import com.sap.cloud.lm.sl.mta.model.Module;
import com.sap.cloud.lm.sl.mta.model.Visitor;

public class ApplicationColorAppender extends Visitor {

    protected static final String RESOURCE_IS_NOT_FOUND_ERROR = "Resource \"{0}\" is not found";

    protected final ApplicationColor applicationColor;
    protected final ApplicationColor deployedMtaColor;

    public ApplicationColorAppender(ApplicationColor deployedMtaColor, ApplicationColor applicationColor) {
        this.applicationColor = applicationColor;
        this.deployedMtaColor = deployedMtaColor;
    }

    @Override
    public void visit(ElementContext context, Module module) {
        Map<String, Object> parameters = new TreeMap<>(module.getParameters());
        parameters.put(SupportedParameters.APP_NAME, computeAppName(module));
        module.setParameters(parameters);
    }

    protected String computeAppName(Module module) {
        Map<String, Object> moduleParameters = module.getParameters();
        String moduleName = module.getName();
        return getAppName(moduleParameters, moduleName);
    }

    protected String getAppName(Map<String, Object> moduleProperties, String moduleName) {
        return moduleProperties.getOrDefault(SupportedParameters.APP_NAME, moduleName) + applicationColor.asSuffix();
    }

}

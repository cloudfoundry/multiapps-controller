package com.sap.cloud.lm.sl.cf.core.helpers.v2;

import java.util.Map;
import java.util.TreeMap;

import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.mta.model.ElementContext;
import com.sap.cloud.lm.sl.mta.model.v2.Module;
import com.sap.cloud.lm.sl.mta.model.v2.RequiredDependency;
import com.sap.cloud.lm.sl.mta.model.v2.Resource;

public class ApplicationColorAppender extends com.sap.cloud.lm.sl.cf.core.helpers.v1.ApplicationColorAppender {

    public ApplicationColorAppender(ApplicationColor deployedMtaColor, ApplicationColor applicationColor) {
        super(deployedMtaColor, applicationColor);
    }

    @Override
    public void visit(ElementContext context, com.sap.cloud.lm.sl.mta.model.v1.Module module) {
        visit(context, (Module) module);
    }

    private void visit(ElementContext context, Module module) {
        Map<String, Object> parameters = new TreeMap<>(module.getParameters());
        parameters.put(SupportedParameters.APP_NAME, computeAppName(module));
        module.setParameters(parameters);
    }

    @Override
    protected String computeAppName(com.sap.cloud.lm.sl.mta.model.v1.Module module) {
        return this.computeAppName((Module) module);
    }

    protected String computeAppName(Module module) {
        Map<String, Object> moduleParameters = module.getParameters();
        String moduleName = module.getName();
        return this.getAppName(moduleParameters, moduleName);
    }

    protected RequiredDependency.Builder getRequiredDependencyBuilder() {
        return new RequiredDependency.Builder();
    }

    @Override
    protected Resource.Builder getResourceBuilder() {
        return new Resource.Builder();
    }

}

package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Map;
import java.util.TreeMap;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.mta.model.ElementContext;
import com.sap.cloud.lm.sl.mta.model.Module;
import com.sap.cloud.lm.sl.mta.model.Visitor;

public class ApplicationNameSuffixAppender extends Visitor {

    private final String suffix;

    public ApplicationNameSuffixAppender(String suffix) {
        this.suffix = suffix;
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
        return moduleProperties.getOrDefault(SupportedParameters.APP_NAME, moduleName) + suffix;
    }

}

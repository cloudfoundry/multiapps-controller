package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Map;
import java.util.TreeMap;

import org.cloudfoundry.multiapps.mta.model.ElementContext;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.Visitor;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;

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

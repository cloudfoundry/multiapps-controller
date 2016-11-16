package com.sap.cloud.lm.sl.cf.core.helpers.v1_0;

import java.util.Map;
import java.util.TreeMap;

import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.mta.model.ElementContext;
import com.sap.cloud.lm.sl.mta.model.Visitor;
import com.sap.cloud.lm.sl.mta.model.v1_0.Module;

public class ApplicationColorAppender extends Visitor {

    private ApplicationColor applicationColor;

    public ApplicationColorAppender(ApplicationColor applicationColor) {
        this.applicationColor = applicationColor;
    }

    @Override
    public void visit(ElementContext context, Module module) {
        Map<String, Object> properties = new TreeMap<>(module.getProperties());
        properties.put(SupportedParameters.APP_NAME, computeAppName(module.getProperties(), module.getName()));
        module.setProperties(properties);
    }

    protected String computeAppName(Map<String, Object> moduleProperties, String moduleName) {
        return (String) moduleProperties.getOrDefault(SupportedParameters.APP_NAME, moduleName) + applicationColor.asSuffix();
    }

}

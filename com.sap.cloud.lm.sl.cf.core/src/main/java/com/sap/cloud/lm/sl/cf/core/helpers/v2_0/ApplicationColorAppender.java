package com.sap.cloud.lm.sl.cf.core.helpers.v2_0;

import java.util.Map;
import java.util.TreeMap;

import com.sap.cloud.lm.sl.cf.core.model.ApplicationColor;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.mta.model.ElementContext;
import com.sap.cloud.lm.sl.mta.model.v2_0.Module;

public class ApplicationColorAppender extends com.sap.cloud.lm.sl.cf.core.helpers.v1_0.ApplicationColorAppender {

    public ApplicationColorAppender(ApplicationColor applicationColor) {
        super(applicationColor);
    }

    @Override
    public void visit(ElementContext context, com.sap.cloud.lm.sl.mta.model.v1_0.Module module) {
        visit(context, (Module) module);
    }

    private void visit(ElementContext context, Module module) {
        Map<String, Object> parameters = new TreeMap<>(module.getParameters());
        parameters.put(SupportedParameters.APP_NAME, computeAppName(module.getParameters(), module.getName()));
        module.setParameters(parameters);
    }

}

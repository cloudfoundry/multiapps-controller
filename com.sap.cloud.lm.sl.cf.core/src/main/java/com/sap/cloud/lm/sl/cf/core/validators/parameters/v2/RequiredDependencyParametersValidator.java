package com.sap.cloud.lm.sl.cf.core.validators.parameters.v2;

import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParametersValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParametersValidatorHelper;
import com.sap.cloud.lm.sl.mta.model.Module;
import com.sap.cloud.lm.sl.mta.model.RequiredDependency;

public class RequiredDependencyParametersValidator extends ParametersValidator<RequiredDependency> {

    protected Module module;
    protected RequiredDependency requiredDependency;

    public RequiredDependencyParametersValidator(String prefix, Module module, RequiredDependency requiredDependency,
        ParametersValidatorHelper helper) {
        super(prefix, requiredDependency.getName(), helper, Module.class);
        this.module = module;
        this.requiredDependency = requiredDependency;
    }

    @Override
    public RequiredDependency validate() {
        Map<String, Object> parameters = validateParameters(module, requiredDependency.getParameters());
        requiredDependency.setParameters(parameters);
        return requiredDependency;
    }

}

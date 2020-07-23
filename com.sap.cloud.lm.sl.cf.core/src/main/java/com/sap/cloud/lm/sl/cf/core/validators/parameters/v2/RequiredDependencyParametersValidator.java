package com.sap.cloud.lm.sl.cf.core.validators.parameters.v2;

import java.util.Map;

import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;

import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParametersValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParametersValidatorHelper;

public class RequiredDependencyParametersValidator extends ParametersValidator<RequiredDependency> {

    protected final RequiredDependency requiredDependency;

    public RequiredDependencyParametersValidator(String prefix, RequiredDependency requiredDependency, ParametersValidatorHelper helper) {
        super(prefix, requiredDependency.getName(), helper, Module.class);
        this.requiredDependency = requiredDependency;
    }

    @Override
    public RequiredDependency validate() {
        Map<String, Object> parameters = validateParameters(requiredDependency.getParameters());
        requiredDependency.setParameters(parameters);
        return requiredDependency;
    }

}

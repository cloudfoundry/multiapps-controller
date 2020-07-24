package org.cloudfoundry.multiapps.controller.core.validators.parameters.v2;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.validators.parameters.ParametersValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.ParametersValidatorHelper;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.RequiredDependency;

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

package com.sap.cloud.lm.sl.cf.core.validators.parameters.v3;

import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParametersValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParametersValidatorHelper;
import com.sap.cloud.lm.sl.mta.model.Module;
import com.sap.cloud.lm.sl.mta.model.ProvidedDependency;

public class ProvidedDependencyParameterValidator extends ParametersValidator<ProvidedDependency> {

    protected Module module;
    protected ProvidedDependency providedDependency;

    public ProvidedDependencyParameterValidator(String prefix, Module module, ProvidedDependency providedDependency,
                                                ParametersValidatorHelper helper) {
        super(prefix, providedDependency.getName(), helper, Module.class);
        this.providedDependency = providedDependency;
    }

    @Override
    public ProvidedDependency validate() {
        Map<String, Object> parameters = validateParameters(module, providedDependency.getParameters());
        providedDependency.setParameters(parameters);
        return providedDependency;
    }

}

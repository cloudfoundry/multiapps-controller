package com.sap.cloud.lm.sl.cf.core.validators.parameters.v2;

import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParametersValidatorHelper;
import com.sap.cloud.lm.sl.mta.model.v2.Module;

public class ModuleParametersValidator extends com.sap.cloud.lm.sl.cf.core.validators.parameters.v1.ModuleParametersValidator {

    protected ModuleParametersValidator(Module module, ParametersValidatorHelper helper) {
        super(module, helper);
    }

    @Override
    public Module validate() {
        Module moduleV2 = (Module) module;
        Map<String, Object> parameters = validateParameters(module, ((Module) module).getParameters());
        moduleV2.setParameters(parameters);
        return moduleV2;
    }

}

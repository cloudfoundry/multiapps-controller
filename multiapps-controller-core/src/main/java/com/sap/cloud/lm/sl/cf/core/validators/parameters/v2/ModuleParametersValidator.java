package com.sap.cloud.lm.sl.cf.core.validators.parameters.v2;

import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParametersValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParametersValidatorHelper;
import com.sap.cloud.lm.sl.mta.model.v2.Module;

public class ModuleParametersValidator extends ParametersValidator<Module> {

    protected Module module;

    protected ModuleParametersValidator(Module module, ParametersValidatorHelper helper) {
        super("", module.getName(), helper, Module.class);
        this.module = module;
    }

    @Override
    public Module validate() {
        Module moduleV2 = module;
        Map<String, Object> parameters = validateParameters(module, module.getParameters());
        moduleV2.setParameters(parameters);
        return moduleV2;
    }

}

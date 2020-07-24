package org.cloudfoundry.multiapps.controller.core.validators.parameters.v2;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.validators.parameters.ParametersValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.ParametersValidatorHelper;
import org.cloudfoundry.multiapps.mta.model.Module;

public class ModuleParametersValidator extends ParametersValidator<Module> {

    protected final Module module;

    protected ModuleParametersValidator(Module module, ParametersValidatorHelper helper) {
        super("", module.getName(), helper, Module.class);
        this.module = module;
    }

    @Override
    public Module validate() {
        Map<String, Object> parameters = validateParameters(module.getParameters());
        module.setParameters(parameters);
        return module;
    }

}

package com.sap.cloud.lm.sl.cf.core.validators.parameters.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParametersValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParametersValidatorHelper;
import com.sap.cloud.lm.sl.mta.model.v1.Module;
import com.sap.cloud.lm.sl.mta.model.v1.ProvidedDependency;

public class ModuleParametersValidator extends ParametersValidator<Module> {

    protected Module module;

    public ModuleParametersValidator(Module module, ParametersValidatorHelper helper) {
        super("", module.getName(), helper, Module.class);
        this.module = module;
    }

    @Override
    public Module validate() {
        List<ProvidedDependency> providedDependencies = validateProvidedDependencies(module.getProvidedDependencies1());
        Map<String, Object> properties = validateParameters(module, module.getProperties());
        module.setProperties(properties);
        module.setProvidedDependencies1(providedDependencies);
        return module;
    }

    protected List<ProvidedDependency> validateProvidedDependencies(List<ProvidedDependency> providedDependencies) {
        List<ProvidedDependency> validProvidedDependencies = new ArrayList<>();
        for (ProvidedDependency providedDependency : providedDependencies) {
            validProvidedDependencies.add(getProvidedDependencyParametersValidator(providedDependency).validate());
        }
        return validProvidedDependencies;
    }

    protected ProvidedDependencyParametersValidator getProvidedDependencyParametersValidator(ProvidedDependency providedDependency) {
        return new ProvidedDependencyParametersValidator(prefix, module, providedDependency, helper);
    }

}

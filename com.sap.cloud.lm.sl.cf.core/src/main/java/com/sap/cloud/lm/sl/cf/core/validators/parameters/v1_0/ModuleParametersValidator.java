package com.sap.cloud.lm.sl.cf.core.validators.parameters.v1_0;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParametersValidator;
import com.sap.cloud.lm.sl.cf.core.validators.parameters.ParametersValidatorHelper;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.v1_0.Module;
import com.sap.cloud.lm.sl.mta.model.v1_0.ProvidedDependency;

public class ModuleParametersValidator extends ParametersValidator<Module> {

    protected Module module;

    public ModuleParametersValidator(Module module, ParametersValidatorHelper helper) {
        super("", module.getName(), helper, Module.class);
        this.module = module;
    }

    @Override
    public Module validate() throws SLException {
        List<ProvidedDependency> providedDependencies = validateProvidedDependencies(module.getProvidedDependencies1_0());
        Map<String, Object> properties = validateParameters(module, module.getProperties());
        module.setProperties(properties);
        module.setProvidedDependencies1_0(providedDependencies);
        return module;
    }

    protected List<ProvidedDependency> validateProvidedDependencies(List<ProvidedDependency> providedDependencies) throws SLException {
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

package org.cloudfoundry.multiapps.controller.core.validators.parameters.v2;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.validators.parameters.ParametersValidator;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.ParametersValidatorHelper;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.ProvidedDependency;

public class ProvidedDependencyParametersValidator extends ParametersValidator<ProvidedDependency> {

    protected final ProvidedDependency providedDependency;

    public ProvidedDependencyParametersValidator(String prefix, ProvidedDependency providedDependency, ParametersValidatorHelper helper) {
        super(prefix, providedDependency.getName(), helper, Module.class);
        this.providedDependency = providedDependency;
    }

    @Override
    public ProvidedDependency validate() {
        Map<String, Object> properties = validateParameters(providedDependency.getProperties());
        providedDependency.setProperties(properties);
        return providedDependency;
    }

}

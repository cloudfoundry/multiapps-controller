package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;

public class IdleRoutesValidator extends RoutesValidator {

    public IdleRoutesValidator(String namespace, boolean applyNamespaceGlobalLevel, Boolean applyNamespaceProcessVariable) {
        super(namespace, applyNamespaceGlobalLevel, applyNamespaceProcessVariable);
    }

    @Override
    protected void initRoutesValidators(String namespace, boolean applyNamespaceGlobalLevel, Boolean applyNamespaceProcessVariable) {
        ParameterValidator idleRouteValidator = new IdleRouteValidator(namespace, applyNamespaceGlobalLevel, applyNamespaceProcessVariable);
        this.validators = Map.of(idleRouteValidator.getParameterName(), idleRouteValidator);
    }

    @Override
    public String getParameterName() {
        return SupportedParameters.IDLE_ROUTES;
    }

}

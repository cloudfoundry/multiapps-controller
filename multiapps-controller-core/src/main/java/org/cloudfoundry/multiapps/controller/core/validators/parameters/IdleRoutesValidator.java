package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;

public class IdleRoutesValidator extends RoutesValidator {

    public IdleRoutesValidator(String namespace, boolean applyNamespaceGlobal, Boolean applyNamespaceOperational) {
        super(namespace, applyNamespaceGlobal, applyNamespaceOperational);
    }

    @Override
    protected void initRoutesValidators(String namespace, boolean applyNamespaceGlobal, Boolean applyNamespaceOperational) {
        ParameterValidator idleRouteValidator = new IdleRouteValidator(namespace, applyNamespaceGlobal, applyNamespaceOperational);
        this.validators = Map.of(idleRouteValidator.getParameterName(), idleRouteValidator);
    }

    @Override
    public String getParameterName() {
        return SupportedParameters.IDLE_ROUTES;
    }

}

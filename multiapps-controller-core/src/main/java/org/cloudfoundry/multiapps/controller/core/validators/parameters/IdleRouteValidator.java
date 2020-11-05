package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;

public class IdleRouteValidator extends RouteValidator {

    public IdleRouteValidator(String namespace, boolean applyNamespaceGlobal) {
        super(namespace, applyNamespaceGlobal);
    }

    @Override
    public String getParameterName() {
        return SupportedParameters.IDLE_ROUTE;
    }

}

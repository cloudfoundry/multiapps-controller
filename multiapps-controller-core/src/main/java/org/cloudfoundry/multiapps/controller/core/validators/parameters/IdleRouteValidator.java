package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;

public class IdleRouteValidator extends RouteValidator {

    public IdleRouteValidator(String namespace, boolean applyNamespaceGlobalLevel, Boolean applyNamespaceProcessVariable,
                              boolean applyNamespaceAsSuffixGlobalLevel, Boolean applyNamespaceAsSuffixProcessVariable) {
        super(namespace,
              applyNamespaceGlobalLevel,
              applyNamespaceProcessVariable,
              applyNamespaceAsSuffixGlobalLevel,
              applyNamespaceAsSuffixProcessVariable);
    }

    @Override
    public String getParameterName() {
        return SupportedParameters.IDLE_ROUTE;
    }

}

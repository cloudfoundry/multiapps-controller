package org.cloudfoundry.multiapps.controller.process.util;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;

import java.util.Collections;
import java.util.Map;

public class NamespaceGlobalParameters extends GlobalParameters {
    private final Map<String, Boolean> applyNamespaceGlobal;

    public NamespaceGlobalParameters(DeploymentDescriptor deploymentDescriptor) {
        super(deploymentDescriptor);
        applyNamespaceGlobal = (Map<String, Boolean>) deploymentDescriptor.getParameters()
                                                                          .getOrDefault(SupportedParameters.APPLY_NAMESPACE,
                                                                                        Collections.emptyMap());
    }

    public boolean getApplyNamespaceAppNamesParameter() {
        return getApplyNamespace(SupportedParameters.APPLY_NAMESPACE_APPS);
    }

    public boolean getApplyNamespaceServiceNamesParameter() {
        return getApplyNamespace(SupportedParameters.APPLY_NAMESPACE_SERVICES);
    }

    public boolean getApplyNamespaceAppRoutesParameter() {
        return getApplyNamespace(SupportedParameters.APPLY_NAMESPACE_ROUTES);

    }

    private boolean getApplyNamespace(String supportedParameters) {
        if (!applyNamespaceGlobal.equals(Collections.emptyMap())) {
            return applyNamespaceGlobal.getOrDefault(supportedParameters, true);
        }
        return true;
    }
}

package org.cloudfoundry.multiapps.controller.process.util;

import static org.cloudfoundry.multiapps.controller.core.Messages.NAMESPACE_PARSING_ERROR_MESSAGE;

import java.util.Collections;
import java.util.Map;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;

public class NamespaceGlobalParameters extends GlobalParameters {
    private final Map<String, Object> applyNamespaceGlobalLevel;

    public NamespaceGlobalParameters(DeploymentDescriptor deploymentDescriptor) {
        super(deploymentDescriptor);
        applyNamespaceGlobalLevel = (Map<String, Object>) deploymentDescriptor.getParameters()
                                                                              .getOrDefault(SupportedParameters.APPLY_NAMESPACE,
                                                                                            Collections.emptyMap());
    }

    public boolean getApplyNamespaceAppNamesParameter() {
        return getApplyNamespace(SupportedParameters.APPLY_NAMESPACE_APPS, Boolean.TRUE);
    }

    public boolean getApplyNamespaceServiceNamesParameter() {
        return getApplyNamespace(SupportedParameters.APPLY_NAMESPACE_SERVICES, Boolean.TRUE);
    }

    public boolean getApplyNamespaceAppRoutesParameter() {
        return getApplyNamespace(SupportedParameters.APPLY_NAMESPACE_ROUTES, Boolean.TRUE);

    }

    public boolean getApplyNamespaceAsSuffix() {
        return getApplyNamespace(SupportedParameters.APPLY_NAMESPACE_AS_SUFFIX, Boolean.FALSE);
    }

    private boolean getApplyNamespace(String applyNamespaceFlagName, Boolean defaultValue) {
        if (!applyNamespaceGlobalLevel.isEmpty()) {
            Object value = applyNamespaceGlobalLevel.get(applyNamespaceFlagName);
            if (value != null && !(value instanceof Boolean)) {
                throw new ContentException(NAMESPACE_PARSING_ERROR_MESSAGE, applyNamespaceFlagName);
            }
            return (boolean) applyNamespaceGlobalLevel.getOrDefault(applyNamespaceFlagName, defaultValue);
        }
        return defaultValue;
    }
}

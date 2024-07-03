package org.cloudfoundry.multiapps.controller.core.util;

import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

import static java.text.MessageFormat.format;

public class NamespaceValidationUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceValidationUtil.class);
    private String namespace;
    private boolean applyNamespaceGlobalLevel;
    private Boolean applyNamespaceProcessVariable;

    public NamespaceValidationUtil() {
    }

    public NamespaceValidationUtil(String namespace, boolean applyNamespaceGlobalLevel, Boolean applyNamespaceProcessVariable) {
        this.namespace = namespace;
        this.applyNamespaceGlobalLevel = applyNamespaceGlobalLevel;
        this.applyNamespaceProcessVariable = applyNamespaceProcessVariable;
    }

    public String getNamespace() {
        return namespace;
    }

    public boolean shouldApplyNamespaceResultValue(final Map<String, Object> relatedParameters) {
        Boolean applyNamespaceModuleLevel = MapUtil.parseBooleanFlag(relatedParameters, SupportedParameters.APPLY_NAMESPACE, null);
        boolean applyNamespaceResult = Objects.requireNonNullElse(applyNamespaceProcessVariable,
                                                                  Objects.requireNonNullElse(applyNamespaceModuleLevel,
                                                                                             applyNamespaceGlobalLevel));
        if (namespace == null && applyNamespaceResult) {
            LOGGER.warn(format(Messages.IGNORING_NAMESPACE_PARAMETERS, SupportedParameters.NAMESPACE));
        }
        return applyNamespaceResult;
    }
}

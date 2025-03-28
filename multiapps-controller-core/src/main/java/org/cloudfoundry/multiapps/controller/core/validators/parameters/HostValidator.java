package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.helpers.SystemParameters;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;

public class HostValidator extends RoutePartValidator {
    public HostValidator() {
        super(null, false, false, false, false);
    }

    public HostValidator(String namespace, boolean applyNamespaceGlobalLevel, Boolean applyNamespaceProcessVariable,
                         boolean applyNamespaceAsSuffixGlobalLevel, Boolean applyNamespaceAsSuffixProcessVariable) {
        super(namespace,
              applyNamespaceGlobalLevel,
              applyNamespaceProcessVariable,
              applyNamespaceAsSuffixGlobalLevel,
              applyNamespaceAsSuffixProcessVariable);
    }

    @Override
    public boolean isValid(Object routePart, final Map<String, Object> context) {
        if (!super.isValid(routePart, context)) {
            return false;
        }
        if (shouldApplyNamespace(context) && getNamespace() != null) {
            if (shouldApplyNamespaceAsSuffix(context)) {
                return isHostWithNamespaceAndSuffixValid((String) routePart);
            }
            return ((String) routePart).startsWith(getNamespace());
        }
        return true;
    }

    private boolean isHostWithNamespaceAndSuffixValid(String routePart) {
        if (routePart.endsWith(getNamespace())) {
            return true;
        } else if (routePart.endsWith(SystemParameters.IDLE_HOST_SUFFIX)) {
            return containsNamespaceBeforeSuffix(routePart, SystemParameters.IDLE_HOST_SUFFIX);
        } else if (routePart.endsWith(SystemParameters.BLUE_HOST_SUFFIX)) {
            return containsNamespaceBeforeSuffix(routePart, SystemParameters.BLUE_HOST_SUFFIX);
        } else if (routePart.endsWith(SystemParameters.GREEN_HOST_SUFFIX)) {
            return containsNamespaceBeforeSuffix(routePart, SystemParameters.GREEN_HOST_SUFFIX);
        } else {
            return false;
        }
    }

    private boolean containsNamespaceBeforeSuffix(String routePart, String suffix) {
        return routePart.substring(0, routePart.lastIndexOf(suffix))
                        .endsWith(getNamespace());
    }

    @Override
    public String getParameterName() {
        return SupportedParameters.HOST;
    }

    @Override
    protected String getErrorMessage() {
        return Messages.COULD_NOT_CREATE_VALID_HOST;
    }

    @Override
    protected int getRoutePartMaxLength() {
        return 63;
    }

    @Override
    protected String getRoutePartIllegalCharacters() {
        return "[^a-z0-9\\-]";
    }

    @Override
    protected String getRoutePartPattern() {
        return "^([a-z0-9]|[a-z0-9][a-z0-9\\-]{0,61}[a-z0-9])|\\*$";
    }

    @Override
    protected String modifyAndShortenRoutePart(String routePart, Map<String, Object> context) {
        return NameUtil.computeNamespacedNameWithLength(routePart, getNamespace(), shouldApplyNamespace(context),
                                                        shouldApplyNamespaceAsSuffix(context), getRoutePartMaxLength());
    }

    private boolean shouldApplyNamespace(Map<String, Object> context) {
        return shouldApplyNamespaceResultValue(context);
    }

    @Override
    public Set<String> getRelatedParameterNames() {
        return Collections.singleton(SupportedParameters.APPLY_NAMESPACE);
    }

}

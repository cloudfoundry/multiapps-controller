package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;

public class HostValidator extends RoutePartValidator {
    public HostValidator() {
        super(null, false, false);
    }

    public HostValidator(String namespace, boolean applyNamespaceGlobalLevel, Boolean applyNamespaceProcessVariable) {
        super(namespace, applyNamespaceGlobalLevel, applyNamespaceProcessVariable);
    }

    @Override
    public boolean isValid(Object routePart, final Map<String, Object> context) {
        if (!super.isValid(routePart, context)) {
            return false;
        }
        if (shouldApplyNamespace(context) && getNamespace() != null) {
            return ((String) routePart).startsWith(getNamespace());
        }
        return true;
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
        return NameUtil.computeNamespacedNameWithLength(routePart, getNamespace(), shouldApplyNamespace(context), getRoutePartMaxLength());
    }

    private boolean shouldApplyNamespace(Map<String, Object> context) {
        return shouldApplyNamespaceResultValue(context);
    }

    @Override
    public Set<String> getRelatedParameterNames() {
        return Collections.singleton(SupportedParameters.APPLY_NAMESPACE);
    }

}

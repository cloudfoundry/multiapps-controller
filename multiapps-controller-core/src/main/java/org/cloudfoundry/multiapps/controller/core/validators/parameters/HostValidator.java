package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.NameUtil;

public class HostValidator extends RoutePartValidator {

    private final String namespace;
    private final boolean applyNamespaceGlobal;

    public HostValidator() {
        this.namespace = null;
        this.applyNamespaceGlobal = false;
    }

    public HostValidator(String namespace, boolean applyNamespaceGlobal) {
        this.namespace = namespace;
        this.applyNamespaceGlobal = applyNamespaceGlobal;
    }

    @Override
    public boolean isValid(Object routePart, final Map<String, Object> context) {
        if (!super.isValid(routePart, context)) {
            return false;
        }
        if (shouldApplyNamespace(context)) {
            return ((String) routePart).startsWith(namespace);
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
        return NameUtil.computeNamespacedNameWithLength(routePart, namespace, shouldApplyNamespace(context), getRoutePartMaxLength());
    }

    private boolean shouldApplyNamespace(Map<String, Object> context) {
        boolean applyNamespaceLocal = MapUtil.parseBooleanFlag(context, SupportedParameters.APPLY_NAMESPACE, true);
        return applyNamespaceGlobal && applyNamespaceLocal;
    }

    @Override
    public Set<String> getRelatedParameterNames() {
        return Collections.singleton(SupportedParameters.APPLY_NAMESPACE);
    }

}

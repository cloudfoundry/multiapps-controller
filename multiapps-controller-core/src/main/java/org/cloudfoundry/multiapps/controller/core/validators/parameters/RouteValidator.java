package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationURI;
import org.cloudfoundry.multiapps.mta.model.Module;

public class RouteValidator implements ParameterValidator {

    private final List<ParameterValidator> validators;

    public RouteValidator(String namespace, boolean applyNamespaceGlobal) {
        this.validators = List.of(new HostValidator(namespace, applyNamespaceGlobal), new DomainValidator());
    }

    @Override
    public String attemptToCorrect(Object route, final Map<String, Object> context) {
        if (!(route instanceof String)) {
            throw new SLException(Messages.COULD_NOT_CREATE_VALID_ROUTE, route);
        }
        String routeString = (String) route;
        boolean noHostname = MapUtil.parseBooleanFlag(context, SupportedParameters.NO_HOSTNAME, false);
        String protocol = (String) context.get(SupportedParameters.ROUTE_PROTOCOL);
        ApplicationURI uri = new ApplicationURI(routeString, noHostname, protocol);
        try {
            for (ParameterValidator validator : validators) {
                correctUriPartIfPresent(uri, validator, context);
            }
        } catch (SLException e) {
            throw new SLException(Messages.COULD_NOT_CREATE_VALID_ROUTE_NESTED_EXCEPTION, route, e.getMessage());
        }

        String correctedRoute = uri.toString();

        if (!isValid(correctedRoute, context)) {
            throw new SLException(Messages.COULD_NOT_CREATE_VALID_ROUTE, route);
        }

        return correctedRoute;
    }

    protected void correctUriPartIfPresent(ApplicationURI uri, ParameterValidator partValidator, Map<String, Object> context) {
        String uriPartName = partValidator.getParameterName();
        Object part = uri.getURIPart(uriPartName);

        if (part == null) {
            return;
        }

        if (partValidator.canCorrect() && !partValidator.isValid(part, context)) {
            String correctedPart = (String) partValidator.attemptToCorrect(part, context);
            uri.setURIPart(uriPartName, correctedPart);
        }
    }

    @Override
    public boolean isValid(Object route, final Map<String, Object> context) {
        if (!(route instanceof String)) {
            return false;
        }

        String routeString = (String) route;
        if (routeString.isEmpty()) {
            return false;
        }

        boolean noHostname = MapUtil.parseBooleanFlag(context, SupportedParameters.NO_HOSTNAME, false);
        String protocol = (String) context.get(SupportedParameters.ROUTE_PROTOCOL);
        ApplicationURI uri = new ApplicationURI(routeString, noHostname, protocol);
        Map<String, Object> uriParts = uri.getURIParts();

        for (ParameterValidator validator : validators) {
            if (!partIsValid(validator, uriParts, context)) {
                return false;
            }
        }

        if (noHostname == false && !uriParts.containsKey(SupportedParameters.HOST)) {
            return false;
        }

        return true;
    }

    protected boolean partIsValid(ParameterValidator validator, Map<String, Object> uriParts, Map<String, Object> context) {
        return !uriParts.containsKey(validator.getParameterName())
            || validator.isValid(uriParts.get(validator.getParameterName()), context);
    }

    @Override
    public String getParameterName() {
        return SupportedParameters.ROUTE;
    }

    @Override
    public Class<?> getContainerType() {
        return Module.class;
    }

    @Override
    public boolean canCorrect() {
        return validators.stream()
                         .map(ParameterValidator::canCorrect)
                         .reduce(false, Boolean::logicalOr);
    }

    @Override
    public Set<String> getRelatedParameterNames() {
        return Collections.singleton(SupportedParameters.NO_HOSTNAME);
    }

}

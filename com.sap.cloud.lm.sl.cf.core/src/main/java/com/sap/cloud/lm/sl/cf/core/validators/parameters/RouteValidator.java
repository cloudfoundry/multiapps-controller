package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.UriUtil;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.v1.Module;

public class RouteValidator implements ParameterValidator {

    private List<ParameterValidator> validators;

    public RouteValidator() {
        this.validators = Arrays.asList(new PortValidator(), new HostValidator(), new DomainValidator());
    }

    @Override
    public String attemptToCorrect(Object route) {
        if (!(route instanceof String)) {
            throw new SLException(Messages.COULD_NOT_CREATE_VALID_ROUTE, route);
        }
        String routeString = (String) route;

        Map<String, Object> uriParts = UriUtil.splitUri(routeString);
        try {
            for (ParameterValidator validator : validators) {
                correctUriPartIfPresent(uriParts, validator);
            }
        } catch (SLException e) {
            throw new SLException(Messages.COULD_NOT_CREATE_VALID_ROUTE_NESTED_EXCEPTION, route, e.getMessage());
        }

        String scheme = UriUtil.getScheme(routeString);
        String path = UriUtil.getPath(routeString);

        String correctedRoute = UriUtil.buildUri(scheme, (String) uriParts.get(SupportedParameters.HOST),
            (String) uriParts.get(SupportedParameters.DOMAIN), (Integer) uriParts.get(SupportedParameters.PORT), path);

        if (!isValid(correctedRoute)) {
            throw new SLException(Messages.COULD_NOT_CREATE_VALID_ROUTE, route);
        }

        return correctedRoute;
    }

    protected void correctUriPartIfPresent(Map<String, Object> uriParts, ParameterValidator partValidator) {
        String uriPartName = partValidator.getParameterName();
        if (!uriParts.containsKey(uriPartName)) {
            return;
        }

        if (partValidator.canCorrect() && !partValidator.isValid(uriParts.get(uriPartName))) {
            String correctedPart = (String) partValidator.attemptToCorrect(uriParts.get(uriPartName));
            uriParts.put(uriPartName, correctedPart);
        }
    }

    @Override
    public boolean isValid(Object route) {
        if (!(route instanceof String)) {
            return false;
        }

        String routeString = (String) route;
        if (routeString.isEmpty()) {
            return false;
        }

        Map<String, Object> uriParts = UriUtil.splitUri(routeString);
        boolean partsAreValid = validators.stream()
            .allMatch(validator -> partIsValid(validator, uriParts));

        boolean hostOrPortPresent = uriParts.containsKey(SupportedParameters.HOST) || uriParts.containsKey(SupportedParameters.PORT);

        return hostOrPortPresent && partsAreValid;
    }

    protected boolean partIsValid(ParameterValidator validator, Map<String, Object> uriParts) {
        return !uriParts.containsKey(validator.getParameterName()) || validator.isValid(uriParts.get(validator.getParameterName()));
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

}

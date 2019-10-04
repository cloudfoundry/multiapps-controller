package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationURI;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.Module;

public class RouteValidator implements ParameterValidator {

    private final List<ParameterValidator> validators;

    public RouteValidator() {
        this.validators = Arrays.asList(new HostValidator(), new DomainValidator());
    }

    @Override
    public String attemptToCorrect(Object route) {
        if (!(route instanceof String)) {
            throw new SLException(Messages.COULD_NOT_CREATE_VALID_ROUTE, route);
        }
        String routeString = (String) route;

        ApplicationURI uri = new ApplicationURI(routeString);
        try {
            for (ParameterValidator validator : validators) {
                correctUriPartIfPresent(uri, validator);
            }
        } catch (SLException e) {
            throw new SLException(Messages.COULD_NOT_CREATE_VALID_ROUTE_NESTED_EXCEPTION, route, e.getMessage());
        }

        String correctedRoute = uri.toString();

        if (!isValid(correctedRoute)) {
            throw new SLException(Messages.COULD_NOT_CREATE_VALID_ROUTE, route);
        }

        return correctedRoute;
    }

    protected void correctUriPartIfPresent(ApplicationURI uri, ParameterValidator partValidator) {
        String uriPartName = partValidator.getParameterName();
        Object part = uri.getURIPart(uriPartName);

        if (part == null) {
            return;
        }

        if (partValidator.canCorrect() && !partValidator.isValid(part)) {
            String correctedPart = (String) partValidator.attemptToCorrect(part);
            uri.setURIPart(uriPartName, correctedPart);
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

        ApplicationURI uri = new ApplicationURI(routeString);
        Map<String, Object> uriParts = uri.getURIParts();
        boolean partsAreValid = validators.stream()
                                          .allMatch(validator -> partIsValid(validator, uriParts));

        boolean hostPresent = uriParts.containsKey(SupportedParameters.HOST);
        return hostPresent && partsAreValid;
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

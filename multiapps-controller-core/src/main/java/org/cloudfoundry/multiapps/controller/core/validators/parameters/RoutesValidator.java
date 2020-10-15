package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.model.Module;

public class RoutesValidator implements ParameterValidator {

    private final Map<String, ParameterValidator> validators;
    private final Set<String> supportedParamsWithoutValidators;

    public RoutesValidator() {
        this.validators = new HashMap<>();
        ParameterValidator routeValidator = new RouteValidator();
        this.validators.put(routeValidator.getParameterName(), routeValidator);

        this.supportedParamsWithoutValidators = Set.of(SupportedParameters.NO_HOSTNAME);
    }

    @Override
    public boolean isValid(Object routes, final Map<String, Object> context) {
        List<Map<String, Object>> routesList = applyRoutesType(routes);

        if (CollectionUtils.isEmpty(routesList)) {
            return false;
        }

        for (Map<String, Object> route : routesList) {
            boolean hasUnsupportedOrInvalidElement = route.entrySet()
                                                          .stream()
                                                          .anyMatch(routeElement -> isElementUnsupportedOrInvalid(routeElement, route));

            if (hasUnsupportedOrInvalidElement) {
                return false;
            }
        }

        return true;
    }

    private boolean isElementUnsupportedOrInvalid(Entry<String, Object> routeElement, final Map<String, Object> allRouteElements) {
        if (supportedParamsWithoutValidators.contains(routeElement.getKey())) {
            return false;
        }

        if (validators.containsKey(routeElement.getKey())) {
            ParameterValidator validator = validators.get(routeElement.getKey());
            return !validator.isValid(routeElement.getValue(), allRouteElements);
        }

        return true;
    }

    @Override
    public Object attemptToCorrect(Object routes, final Map<String, Object> context) {
        List<Map<String, Object>> routesList = applyRoutesType(routes);

        if (CollectionUtils.isEmpty(routesList)) {
            throw new SLException(Messages.COULD_NOT_PARSE_ROUTE);
        }

        return routesList.stream()
                         .map(this::attemptToCorrectParameterMap)
                         .collect(Collectors.toList());
    }

    private Map<String, Object> attemptToCorrectParameterMap(Map<String, Object> originalElem) {
        Map<String, Object> result = new TreeMap<>();
        for (Entry<String, Object> entry : originalElem.entrySet()) {
            if (supportedParamsWithoutValidators.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }

            if (validators.containsKey(entry.getKey())) {
                Object value = attemptToCorrectParameter(validators.get(entry.getKey()), entry.getValue(), originalElem);
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    private Object attemptToCorrectParameter(ParameterValidator validator, Object parameter, final Map<String, Object> context) {
        if (validator.isValid(parameter, context)) {
            return parameter;
        }

        if (validator.canCorrect()) {
            return validator.attemptToCorrect(parameter, context);
        }

        throw new SLException(Messages.COULD_NOT_CREATE_VALID_ROUTE, parameter);
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> applyRoutesType(Object routes) {
        if (routes instanceof List) {
            List<Object> routesList = (List<Object>) routes;
            if (CollectionUtils.isEmpty(routesList)) {
                return Collections.emptyList();
            }

            if (routesList.stream()
                          .anyMatch(route -> !(route instanceof Map))) {
                return Collections.emptyList();
            }
            return routesList.stream()
                             .map(route -> (Map<String, Object>) route)
                             .collect(Collectors.toList());
        }

        return Collections.emptyList();

    }

    @Override
    public Class<?> getContainerType() {
        return Module.class;
    }

    @Override
    public String getParameterName() {
        return SupportedParameters.ROUTES;
    }

    @Override
    public boolean canCorrect() {
        return true;
    }

}

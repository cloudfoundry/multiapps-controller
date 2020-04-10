package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.sap.cloud.lm.sl.cf.core.Messages;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.Module;

public class RoutesValidator implements ParameterValidator {

    private final Map<String, ParameterValidator> validators;

    public RoutesValidator() {
        this.validators = new HashMap<>();
        ParameterValidator routeValidator = new RouteValidator();
        this.validators.put(routeValidator.getParameterName(), routeValidator);
    }

    @Override
    public boolean isValid(Object routes) {
        List<Map<String, Object>> routesList = applyRoutesType(routes);

        if (CollectionUtils.isEmpty(routesList)) {
            return false;
        }

        for (Map<String, Object> routesElement : routesList) {
            boolean hasUnsupportedOrInvalidElement = routesElement.entrySet()
                                                                  .stream()
                                                                  .anyMatch(this::isElementUnsupportedOrInvalid);
            if (hasUnsupportedOrInvalidElement) {
                return false;
            }
        }

        return true;
    }

    private boolean isElementUnsupportedOrInvalid(Entry<String, Object> entry) {
        ParameterValidator validator = validators.get(entry.getKey());
        return validator == null || !validator.isValid(entry.getValue());
    }

    @Override
    public Object attemptToCorrect(Object routes) {
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
            if (validators.containsKey(entry.getKey())) {
                Object value = attemptToCorrectParameter(validators.get(entry.getKey()), entry.getValue());
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    private Object attemptToCorrectParameter(ParameterValidator validator, Object parameter) {
        if (validator.isValid(parameter)) {
            return parameter;
        }

        if (validator.canCorrect()) {
            return validator.attemptToCorrect(parameter);
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

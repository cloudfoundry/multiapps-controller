package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.mta.model.v2.Module;

public class RoutesValidator implements ParameterValidator {

    private final Map<String, ParameterValidator> validators;

    public RoutesValidator() {
        this.validators = new HashMap<String, ParameterValidator>();
        ParameterValidator routeValidator = new RouteValidator();
        this.validators.put(routeValidator.getParameterName(), routeValidator);
    }

    @Override
    public boolean isValid(Object routes) {
        List<Map<String, Object>> routesList = applyRoutesType(routes);

        if (routesList == null) {
            return false;
        }

        for (Map<String, Object> routesElement : routesList) {

            boolean hasUnsupportedOrInvalidElement = routesElement.keySet()
                .stream()
                .anyMatch(key -> validators.get(key) == null || !validators.get(key)
                    .isValid(routesElement.get(key)));

            if (hasUnsupportedOrInvalidElement) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Object attemptToCorrect(Object routes) {
        List<Map<String, Object>> routesList = applyRoutesType(routes);

        if (routesList == null) {
            throw new SLException(Messages.COULD_NOT_PARSE_ROUTE);
        }

        return routesList.stream()
            .map(route -> attemptToCorrectParameterMap(route))
            .collect(Collectors.toList());
    }

    private Map<String, Object> attemptToCorrectParameterMap(Map<String, Object> originalElem) {
        Map<String, Object> correctedElem = new TreeMap<String, Object>();

        for (String key : originalElem.keySet()) {
            if (validators.containsKey(key)) {
                correctedElem.put(key, attemptToCorrectParameter(validators.get(key), originalElem.get(key)));
            }
        }

        return correctedElem;
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
    private List<Map<String, Object>> applyRoutesType(Object routes) {
        if (routes instanceof List) {
            List<Map<String, Object>> routesList = (List<Map<String, Object>>) routes;
            if (CollectionUtils.isEmpty(routesList)) {
                return new ArrayList<Map<String, Object>>();
            }

            if (routesList.stream()
                .anyMatch(route -> !(route instanceof Map))) {
                return null;
            }

            return (List<Map<String, Object>>) routes;
        }

        return null;

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

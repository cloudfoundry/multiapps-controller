package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.model.Module;

public class RoutesValidator implements ParameterValidator {

    protected Map<String, ParameterValidator> validators;

    public RoutesValidator(String namespace, boolean applyNamespaceGlobal) {
        initRoutesValidators(namespace, applyNamespaceGlobal);
    }

    protected void initRoutesValidators(String namespace, boolean applyNamespaceGlobal) {
        ParameterValidator routeValidator = new RouteValidator(namespace, applyNamespaceGlobal);
        this.validators = Map.of(routeValidator.getParameterName(), routeValidator);
    }

    @Override
    public boolean isValid(Object routes, final Map<String, Object> context) {
        List<Map<String, Object>> routesList = applyRoutesType(routes);

        if (CollectionUtils.isEmpty(routesList)) {
            return false;
        }

        for (Map<String, Object> route : routesList) {
            Map<String, Object> updatedAllRouteElements = getUpdatedAllRouteElements(route, context);
            boolean hasUnsupportedOrInvalidElement = route.entrySet()
                                                          .stream()
                                                          .anyMatch(routeElement -> isElementUnsupportedOrInvalid(routeElement,
                                                                                                                  updatedAllRouteElements));

            if (hasUnsupportedOrInvalidElement) {
                return false;
            }
        }

        return true;
    }

    private boolean isElementUnsupportedOrInvalid(Entry<String, Object> routeElement, final Map<String, Object> allRouteElements) {
        if (getRelatedParameterNames().contains(routeElement.getKey())) {
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
                         .map(originalElement -> getUpdatedAllRouteElements(originalElement, context))
                         .map(this::attemptToCorrectParameterMap)
                         .collect(Collectors.toList());
    }

    private Map<String, Object> getUpdatedAllRouteElements(Map<String, Object> routeElement, Map<String, Object> context) {
        Map<String, Object> updatedAllRouteElements = new HashMap<>(routeElement);
        Set<String> relatedParameters = getRelatedParameterNames();
        Map<String, Object> globalParametersValues = relatedParameters.stream()
                                                                      .filter(relatedParameter -> isGlobalParameterValueNotOverridenInRouteElement(relatedParameter,
                                                                                                                                                   routeElement,
                                                                                                                                                   context))
                                                                      .filter(relatedParameter -> !getSpecificRouteParameterNames().contains(relatedParameter))
                                                                      .collect(Collectors.toMap(Function.identity(), context::get));
        updatedAllRouteElements.putAll(globalParametersValues);
        return updatedAllRouteElements;
    }

    private boolean isGlobalParameterValueNotOverridenInRouteElement(String relatedParameter, Map<String, Object> routeElement,
                                                                     Map<String, Object> context) {
        return !routeElement.containsKey(relatedParameter) && context.containsKey(relatedParameter);
    }

    private Map<String, Object> attemptToCorrectParameterMap(Map<String, Object> originalElement) {
        Map<String, Object> result = new TreeMap<>();
        Set<String> supportedParamsWithoutValidators = getRelatedParameterNames();
        for (Entry<String, Object> entry : originalElement.entrySet()) {
            if (supportedParamsWithoutValidators.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }

            if (validators.containsKey(entry.getKey())) {
                Object value = attemptToCorrectParameter(validators.get(entry.getKey()), entry.getValue(), originalElement);
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

    @Override
    public Set<String> getRelatedParameterNames() {
        return Set.of(SupportedParameters.NO_HOSTNAME, SupportedParameters.ROUTE_PROTOCOL, SupportedParameters.APPLY_NAMESPACE);
    }

    private Set<String> getSpecificRouteParameterNames() {
        return Set.of(SupportedParameters.ROUTE_PROTOCOL);
    }

}

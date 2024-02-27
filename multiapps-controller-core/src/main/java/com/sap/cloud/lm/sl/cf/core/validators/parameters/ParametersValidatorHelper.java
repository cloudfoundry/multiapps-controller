package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.mta.util.ValidatorUtil;

public class ParametersValidatorHelper {

    private static final String PARAMETER_CONTAINING_XSA_PLACEHOLDER_PATTERN = ".*?\\{xsa-placeholder-.*\\}.*?";

    private final List<ParameterValidator> parameterValidators;
    private final boolean doNotCorrect;

    public ParametersValidatorHelper(List<ParameterValidator> parameterValidators, boolean doNotCorrect) {
        this.parameterValidators = parameterValidators;
        this.doNotCorrect = doNotCorrect;
    }

    public Map<String, Object> validate(String prefix, Object container, Class<?> containerClass, Map<String, Object> parameters) {
        Map<String, Object> correctedParameters = new TreeMap<>();
        for (ParameterValidator validator : parameterValidators) {
            if (!validator.getContainerType()
                          .isAssignableFrom(containerClass)) {
                continue;
            }

            correctedParameters = correctInvalidSingleParameters(prefix, container, validator, parameters, correctedParameters);
            correctedParameters = correctInvalidPluralParameters(prefix, container, validator, parameters, correctedParameters);
        }
        return MapUtil.merge(parameters, correctedParameters);
    }

    private Map<String, Object> correctInvalidSingleParameters(String prefix, Object container, ParameterValidator validator,
                                                               Map<String, Object> parameters, Map<String, Object> correctedParameters) {
        String parameterName = validator.getParameterName();

        Object initialParameterValue = parameters.get(parameterName);
        Object correctParameterValue = validateAndCorrect(container, ValidatorUtil.getPrefixedName(prefix, parameterName),
                                                          initialParameterValue, validator);
        if (!Objects.equals(initialParameterValue, correctParameterValue)) {
            correctedParameters.put(parameterName, correctParameterValue);
        }

        return correctedParameters;
    }

    private Map<String, Object> correctInvalidPluralParameters(String prefix, Object container, ParameterValidator validator,
                                                               Map<String, Object> parameters, Map<String, Object> correctedParameters) {
        String parameterPluralName = SupportedParameters.SINGULAR_PLURAL_MAPPING.get(validator.getParameterName());

        if (parameterPluralName == null || !parameters.containsKey(parameterPluralName)) {
            return correctedParameters;
        }

        @SuppressWarnings("unchecked")
        List<Object> initialParameterValues = (List<Object>) parameters.get(parameterPluralName);
        if (initialParameterValues == null || initialParameterValues.isEmpty()) {
            return correctedParameters;
        }

        List<Object> correctedParameterValues = initialParameterValues.stream()
                                                                      .map(parameter -> validateAndCorrect(container,
                                                                                                           ValidatorUtil.getPrefixedName(prefix,
                                                                                                                                         validator.getParameterName()),
                                                                                                           parameter, validator))
                                                                      .collect(Collectors.toList());
        correctedParameters.put(parameterPluralName, correctedParameterValues);

        return correctedParameters;
    }

    private Object validateAndCorrect(Object container, String parameterName, Object parameter, ParameterValidator validator) {
        if ((parameter instanceof String) && containsXsaPlaceholders((String) parameter)) {
            return parameter;
        } else if (!validator.isValid(container, parameter)) {
            return attemptToCorrect(container, parameterName, parameter, validator);
        } else {
            return parameter;
        }
    }

    private boolean containsXsaPlaceholders(String parameter) {
        return parameter.matches(PARAMETER_CONTAINING_XSA_PLACEHOLDER_PATTERN);
    }

    private Object attemptToCorrect(Object container, String parameterName, Object parameter, ParameterValidator validator) {
        if (!validator.canCorrect() || doNotCorrect) {
            throw new ContentException(Messages.CANNOT_CORRECT_PARAMETER, parameterName);
        }
        return validator.attemptToCorrect(container, parameter);
    }

}

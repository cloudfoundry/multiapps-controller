package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.ContentException;
import com.sap.cloud.lm.sl.common.SLException;
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

    public Map<String, Object> validate(String prefix, Object container, Class<?> containerClass, Map<String, Object> parameters)
        throws SLException {
        Map<String, Object> validParameters = new TreeMap<String, Object>();
        for (ParameterValidator validator : parameterValidators) {
            if (!validator.getContainerType()
                .isAssignableFrom(containerClass)) {
                continue;
            }
            Object initialParameterValue = parameters.get(validator.getParameterName());
            Object correctParameterValue = validate(container, ValidatorUtil.getPrefixedName(prefix, validator.getParameterName()),
                initialParameterValue, validator);
            if (!Objects.equals(initialParameterValue, correctParameterValue)) {
                validParameters.put(validator.getParameterName(), correctParameterValue);
            }
        }
        return MapUtil.merge(parameters, validParameters);
    }

    private Object validate(Object container, String parameterName, Object parameter, ParameterValidator validator) throws SLException {
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

    private Object attemptToCorrect(Object container, String parameterName, Object parameter, ParameterValidator validator)
        throws SLException {
        if (!validator.canCorrect() || doNotCorrect) {
            throw new ContentException(Messages.CANNOT_CORRECT_PARAMETER, parameterName);
        }
        return validator.attemptToCorrect(container, parameter);
    }

}

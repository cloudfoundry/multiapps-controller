package com.sap.cloud.lm.sl.cf.core.validators.parameters;

import com.sap.cloud.lm.sl.cf.core.Constants;

public interface ParameterValidator {

    default boolean isValid(Object parameter) {
        return true;
    }

    default boolean canCorrect() {
        return false;
    }

    default Object attemptToCorrect(Object parameter) {
        throw new UnsupportedOperationException();
    }

    default boolean containsXsaPlaceholders(Object parameter) {
        if (parameter instanceof String) {
            return ((String) parameter).matches(Constants.PARAMETER_CONTAINING_XSA_PLACEHOLDER_PATTERN);
        }

        return false;
    }

    Class<?> getContainerType();

    String getParameterName();

}

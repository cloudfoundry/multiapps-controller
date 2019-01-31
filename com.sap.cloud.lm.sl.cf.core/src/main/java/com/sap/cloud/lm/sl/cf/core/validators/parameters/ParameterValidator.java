package com.sap.cloud.lm.sl.cf.core.validators.parameters;

public interface ParameterValidator {
    String PARAMETER_CONTAINING_XSA_PLACEHOLDER_PATTERN = ".*?\\{xsa-placeholder-.*\\}.*?";

    default boolean isValid(Object container, Object parameter) {
        if (parameter != null) {
            return isValid(parameter);
        }
        return true;
    }

    default boolean isValid(Object parameter) {
        return true;
    }

    default boolean canCorrect() {
        return false;
    }

    default Object attemptToCorrect(Object container, Object parameter) {
        return attemptToCorrect(parameter);
    }

    default Object attemptToCorrect(Object parameter) {
        throw new UnsupportedOperationException();
    }

    default boolean containsXsaPlaceholders(Object parameter) {
        if (parameter instanceof String) {
            return ((String) parameter).matches(PARAMETER_CONTAINING_XSA_PLACEHOLDER_PATTERN);
        }

        return false;
    }

    Class<?> getContainerType();

    String getParameterName();

}

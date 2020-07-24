package org.cloudfoundry.multiapps.controller.core.validators.parameters;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public interface ParameterValidator {

    default boolean isValid(Object parameter, final Map<String, Object> relatedParameters) {
        return true;
    }

    default boolean canCorrect() {
        return false;
    }

    default Object attemptToCorrect(Object parameter, final Map<String, Object> relatedParameters) {
        throw new UnsupportedOperationException();
    }

    default Set<String> getRelatedParameterNames() {
        return Collections.emptySet();
    }

    String getParameterName();

    Class<?> getContainerType();
}

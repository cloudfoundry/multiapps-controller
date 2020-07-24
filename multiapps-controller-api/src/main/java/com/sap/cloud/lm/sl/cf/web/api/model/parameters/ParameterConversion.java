package com.sap.cloud.lm.sl.cf.web.api.model.parameters;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sap.cloud.lm.sl.cf.web.api.model.ParameterMetadata;

public final class ParameterConversion {

    private ParameterConversion() {
    }

    public static Map<String, Object> toFlowableVariables(Set<ParameterMetadata> parametersMetadata, Map<String, Object> parameters) {
        // Do not refactor this loop into a stream. Collectors.toMap has a known bug (https://bugs.openjdk.java.net/browse/JDK-8148463) and
        // throws NullPointerExceptions when trying to insert null values in the map.
        Map<String, Object> variables = new HashMap<>();
        for (ParameterMetadata parameterMetadata : parametersMetadata) {
            variables.put(parameterMetadata.getId(), toFlowableVariable(parameterMetadata, parameters));
        }
        return variables;
    }

    private static Object toFlowableVariable(ParameterMetadata parameterMetadata, Map<String, Object> parameters) {
        Object value = parameters.computeIfAbsent(parameterMetadata.getId(), key -> parameterMetadata.getDefaultValue());
        return value == null ? null : getParameterConverter(parameterMetadata).convert(value);
    }

    private static ParameterConverter getParameterConverter(ParameterMetadata parameterMetadata) {
        if (parameterMetadata.getCustomConverter() != null) {
            return parameterMetadata.getCustomConverter();
        }
        switch (parameterMetadata.getType()) {
            case BOOLEAN:
                return new BooleanParameterConverter();
            case INTEGER:
                return new IntegerParameterConverter();
            case STRING:
                return new StringParameterConverter();
            case TABLE:
                throw new UnsupportedOperationException();
            default:
                throw new IllegalStateException(MessageFormat.format("Unknown parameter type: {0}", parameterMetadata.getType()));

        }
    }

}

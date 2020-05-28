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
        Map<String, Object> variables = new HashMap<>();
        for (ParameterMetadata parameterMetadata : parametersMetadata) {
            if (!parameters.containsKey(parameterMetadata.getId())) {
                continue;
            }
            Object value = parameters.get(parameterMetadata.getId());

            ParameterConverter parameterConverter = getParameterConverter(parameterMetadata);
            variables.put(parameterMetadata.getId(), parameterConverter.convert(value));
        }
        return variables;
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

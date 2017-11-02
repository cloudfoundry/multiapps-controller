package com.sap.cloud.lm.sl.cf.web.api.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sap.cloud.lm.sl.cf.web.api.model.ParameterMetadata.ParameterType;

public class ParameterTypeFactory {
    private Map<String, Object> operationParameters;
    private Set<ParameterMetadata> operationParametersMetadata;

    public ParameterTypeFactory(Map<String, Object> operationParameters, Set<ParameterMetadata> operationParametersMetadata) {
        this.operationParameters = operationParameters;
        this.operationParametersMetadata = operationParametersMetadata;
    }

    public Map<String, Object> getParametersValues() {
        Map<String, Object> resultParameters = new HashMap<>();
        for (ParameterMetadata parameterMetadata : operationParametersMetadata) {
            if (!operationParameters.containsKey(parameterMetadata.getId())) {
                continue;
            }
            Object parameterValue = operationParameters.get(parameterMetadata.getId());

            ParameterTypeProvider typeCaster = getParameterTypeCaster(parameterMetadata.getType());
            resultParameters.put(parameterMetadata.getId(), typeCaster.getParameterType(parameterValue));
        }
        return resultParameters;
    }

    private ParameterTypeProvider getParameterTypeCaster(ParameterType parameterType) {
        if (parameterType == ParameterType.INTEGER) {
            return new IntegerParameterTypeProvider();
        }
        if (parameterType == ParameterType.BOOLEAN) {
            return new BooleanParameterTypeProvider();
        }

        return new StringParameterTypeProvider();
    }
}

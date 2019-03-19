package com.sap.cloud.lm.sl.cf.core.parser;

import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertyValue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;

public class ApplicationAttributeUpdateStrategyParser implements ParametersParser<CloudApplicationExtended.AttributeUpdateStrategy> {

    @Override
    public CloudApplicationExtended.AttributeUpdateStrategy parse(List<Map<String, Object>> parametersList) {
        Map<String, Boolean> attributeUpdateStrategyMap = getApplicationAttributesUpdateStrategyMap(parametersList);
        return new CloudApplicationExtended.AttributeUpdateStrategy.Builder()
            .shouldKeepExistingEnv(
                attributeUpdateStrategyMap.get(SupportedParameters.ApplicationUpdateStarategy.ENV_APPLICATION_ATTRIBUTES_UPDATE_STRATEGY))
            .shouldKeepExistingRoutes(attributeUpdateStrategyMap.get(SupportedParameters.ROUTES))
            .shouldKeepExistingServiceBindings(attributeUpdateStrategyMap
                .get(SupportedParameters.ApplicationUpdateStarategy.SERVICE_BINDINGS_APPLICATION_ATTRIBUTES_UPDATE_STRATEGY))
            .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Boolean> getApplicationAttributesUpdateStrategyMap(List<Map<String, Object>> parametersList) {
        return (Map<String, Boolean>) getPropertyValue(parametersList,
            SupportedParameters.KEEP_EXISTING_APPLICATION_ATTRIBUTES_UPDATE_STRATEGY, Collections.emptyMap());
    }

}

package com.sap.cloud.lm.sl.cf.core.parser;

import static com.sap.cloud.lm.sl.mta.util.PropertiesUtil.getPropertyValue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudApplicationExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudApplicationExtended.ImmutableAttributeUpdateStrategy;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;

public class ApplicationAttributeUpdateStrategyParser implements ParametersParser<CloudApplicationExtended.AttributeUpdateStrategy> {

    @Override
    public CloudApplicationExtended.AttributeUpdateStrategy parse(List<Map<String, Object>> parametersList) {
        Map<String, Boolean> attributesUpdateStrategy = getAttributesUpdateStrategyParameter(parametersList);
        ImmutableAttributeUpdateStrategy.Builder builder = ImmutableAttributeUpdateStrategy.builder();
        Boolean shouldKeepExistingEnv = shouldKeepExistingEnv(attributesUpdateStrategy);
        Boolean shouldKeepExistingRoutes = shouldKeepExistingRoutes(attributesUpdateStrategy);
        Boolean shouldKeepExistingServiceBindings = shouldKeepExistingServiceBindings(attributesUpdateStrategy);
        if (shouldKeepExistingEnv != null) {
            builder.shouldKeepExistingEnv(shouldKeepExistingEnv);
        }
        if (shouldKeepExistingRoutes != null) {
            builder.shouldKeepExistingRoutes(shouldKeepExistingRoutes);
        }
        if (shouldKeepExistingServiceBindings != null) {
            builder.shouldKeepExistingServiceBindings(shouldKeepExistingServiceBindings);
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Boolean> getAttributesUpdateStrategyParameter(List<Map<String, Object>> parametersList) {
        return (Map<String, Boolean>) getPropertyValue(parametersList,
                                                       SupportedParameters.KEEP_EXISTING_APPLICATION_ATTRIBUTES_UPDATE_STRATEGY,
                                                       Collections.emptyMap());
    }

    private Boolean shouldKeepExistingEnv(Map<String, Boolean> attributesUpdateStrategy) {
        return attributesUpdateStrategy.get(SupportedParameters.ApplicationUpdateStarategy.ENV_APPLICATION_ATTRIBUTES_UPDATE_STRATEGY);
    }

    private Boolean shouldKeepExistingRoutes(Map<String, Boolean> attributesUpdateStrategy) {
        return attributesUpdateStrategy.get(SupportedParameters.ROUTES);
    }

    private Boolean shouldKeepExistingServiceBindings(Map<String, Boolean> attributesUpdateStrategy) {
        return attributesUpdateStrategy.get(SupportedParameters.ApplicationUpdateStarategy.SERVICE_BINDINGS_APPLICATION_ATTRIBUTES_UPDATE_STRATEGY);
    }

}

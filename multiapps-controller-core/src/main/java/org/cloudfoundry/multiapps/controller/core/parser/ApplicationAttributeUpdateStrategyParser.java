package org.cloudfoundry.multiapps.controller.core.parser;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.mta.util.PropertiesUtil;

public class ApplicationAttributeUpdateStrategyParser implements ParametersParser<CloudApplicationExtended.AttributeUpdateStrategy> {

    @Override
    public CloudApplicationExtended.AttributeUpdateStrategy parse(List<Map<String, Object>> parametersList) {
        Map<String, Boolean> attributesUpdateStrategy = getAttributesUpdateStrategyParameter(parametersList);
        ImmutableCloudApplicationExtended.AttributeUpdateStrategy.Builder builder = ImmutableCloudApplicationExtended.AttributeUpdateStrategy.builder();
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
        return (Map<String, Boolean>) PropertiesUtil.getPropertyValue(parametersList,
                                                                      SupportedParameters.KEEP_EXISTING_APPLICATION_ATTRIBUTES_UPDATE_STRATEGY,
                                                                      Collections.emptyMap());
    }

    private Boolean shouldKeepExistingEnv(Map<String, Boolean> attributesUpdateStrategy) {
        return attributesUpdateStrategy.get(SupportedParameters.ApplicationUpdateStrategy.ENV_APPLICATION_ATTRIBUTES_UPDATE_STRATEGY);
    }

    private Boolean shouldKeepExistingRoutes(Map<String, Boolean> attributesUpdateStrategy) {
        return attributesUpdateStrategy.get(SupportedParameters.ROUTES);
    }

    private Boolean shouldKeepExistingServiceBindings(Map<String, Boolean> attributesUpdateStrategy) {
        return attributesUpdateStrategy.get(SupportedParameters.ApplicationUpdateStrategy.SERVICE_BINDINGS_APPLICATION_ATTRIBUTES_UPDATE_STRATEGY);
    }

}

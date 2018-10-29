package com.sap.cloud.lm.sl.cf.core.cf.v1;

import java.util.HashSet;
import java.util.Set;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;

public enum SpecialResourceTypesRequiredParameters {

    MANAGED_SERVICE_REQUIRED_PARAMETERS(ResourceType.MANAGED_SERVICE, SupportedParameters.SERVICE,
        SupportedParameters.SERVICE_PLAN), USER_PROVIDED_SERVICE_REQUIRED_PARAMETERS(ResourceType.USER_PROVIDED_SERVICE,
            SupportedParameters.SERVICE_CONFIG), EXISTING_SERVICE_REQUIRED_PARAMETERS(
                ResourceType.EXISTING_SERVICE), EXISTING_SERVICE_KEY_REQUIRED_PARAMETERS(ResourceType.EXISTING_SERVICE_KEY);

    private final ResourceType resourceType;
    private final HashSet<String> requiredParameters = new HashSet<>();

    SpecialResourceTypesRequiredParameters(ResourceType resourceType, String... requiredParameter) {
        this.resourceType = resourceType;
        for (String supportedParameters : requiredParameter) {
            requiredParameters.add(supportedParameters);
        }
    }

    public static Set<String> getRequiredParameters(ResourceType resourceType) {
        for (SpecialResourceTypesRequiredParameters value : SpecialResourceTypesRequiredParameters.values()) {
            if (value.resourceType.equals(resourceType)) {
                return value.requiredParameters;
            }
        }
        return new HashSet<>();
    }

}

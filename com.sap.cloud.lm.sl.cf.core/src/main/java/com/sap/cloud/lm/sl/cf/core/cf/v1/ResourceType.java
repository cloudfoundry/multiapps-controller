package com.sap.cloud.lm.sl.cf.core.cf.v1;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;

public enum ResourceType {
    MANAGED_SERVICE("managed-service", SupportedParameters.SERVICE, SupportedParameters.SERVICE_PLAN), USER_PROVIDED_SERVICE(
        "user-provided-service",
        SupportedParameters.SERVICE_CONFIG), EXISTING_SERVICE("existing-service"), EXISTING_SERVICE_KEY("existing-service-key");

    private String name;
    private final HashSet<String> requiredParameters = new HashSet<>();

    private ResourceType(String value, String... requiredParameters) {
        this.name = value;
        for (String supportedParameters : requiredParameters) {
            this.requiredParameters.add(supportedParameters);
        }
    }

    @Override
    public String toString() {
        return name;
    }

    public static ResourceType get(String name) {
        for (ResourceType value : values()) {
            if (value.name.equals(name))
                return value;
        }
        return null;
    }

    public static Set<ResourceType> getServiceTypes() {
        return EnumSet.of(MANAGED_SERVICE, USER_PROVIDED_SERVICE, EXISTING_SERVICE);
    }

    public HashSet<String> getRequiredParameters() {
        return requiredParameters;
    }
}
package com.sap.cloud.lm.sl.cf.core.cf.v1;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;

public enum ResourceType {
    MANAGED_SERVICE("managed-service", SupportedParameters.SERVICE, SupportedParameters.SERVICE_PLAN), USER_PROVIDED_SERVICE(
        "user-provided-service"), EXISTING_SERVICE("existing-service"), EXISTING_SERVICE_KEY("existing-service-key");

    private String name;
    private final Set<String> requiredParameters = new HashSet<>();

    private ResourceType(String value, String... requiredParameters) {
        this.name = value;
        for (String requiredParameter : requiredParameters) {
            this.requiredParameters.add(requiredParameter);
        }
    }

    @Override
    public String toString() {
        return name;
    }

    public static ResourceType get(String value) {
        for (ResourceType v : values()) {
            if (v.name.equals(value))
                return v;
        }
        return null;
    }

    public static Set<ResourceType> getServiceTypes() {
        return EnumSet.of(MANAGED_SERVICE, USER_PROVIDED_SERVICE, EXISTING_SERVICE);
    }

    public Set<String> getRequiredParameters() {
        return requiredParameters;
    }
    
}
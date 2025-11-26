package org.cloudfoundry.multiapps.controller.core.cf.v2;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;

public enum ResourceType {
    MANAGED_SERVICE("managed-service", SupportedParameters.SERVICE, SupportedParameters.SERVICE_PLAN), USER_PROVIDED_SERVICE(
        "user-provided-service"), EXISTING_SERVICE("existing-service"), EXISTING_SERVICE_KEY(
        "existing-service-key"), EXTERNAL_LOGGING_SERVICE(
        "external-logging-service", SupportedParameters.SERVICE_KEY_NAME);

    private final String name;
    private final Set<String> requiredParameters = new HashSet<>();

    ResourceType(String value, String... requiredParameters) {
        this.name = value;
        Collections.addAll(this.requiredParameters, requiredParameters);
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
package org.cloudfoundry.multiapps.controller.client.facade.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Type of a Cloud Controller v3 service instance. Project-owned replacement for
 * {@code org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceType}, so the domain model no longer depends on the OSS
 * cf-java-client. Values mirror the CF v3 service instance {@code type} field, and JSON (de)serialization uses the wire value
 * ({@code managed} / {@code user-provided}) via {@link JsonValue} / {@link JsonCreator} — matching the OSS enum's behaviour so persisted
 * models round-trip identically.
 */
public enum ServiceInstanceType {

    MANAGED("managed"), USER_PROVIDED("user-provided");

    private final String value;

    ServiceInstanceType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Resolve from the CF v3 wire value ({@code managed} / {@code user-provided}). Mirrors the OSS {@code ServiceInstanceType.from}.
     */
    @JsonCreator
    public static ServiceInstanceType from(String value) {
        for (ServiceInstanceType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown service instance type: " + value);
    }
}

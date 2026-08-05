package org.cloudfoundry.multiapps.controller.client.facade.domain;

public enum HealthCheckType {
    HTTP, PORT, PROCESS, @Deprecated NONE;

    /**
     * The CF v3 wire value (lowercase), matching the OSS {@code HealthCheckType.getValue()} this enum replaces at call sites.
     */
    public String getValue() {
        return name().toLowerCase();
    }

    @Override
    public String toString() {
        return this.name()
                   .toLowerCase();
    }
}

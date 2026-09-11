package org.cloudfoundry.multiapps.controller.client.facade.domain;

public enum HealthCheckType {
    HTTP, PORT, PROCESS, @Deprecated NONE;

    public String getValue() {
        return name().toLowerCase();
    }

    @Override
    public String toString() {
        return this.name()
                   .toLowerCase();
    }
}

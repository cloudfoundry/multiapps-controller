package org.cloudfoundry.multiapps.controller.client.facade.domain;

public enum ServicePlanVisibility {

    PUBLIC, ADMIN, ORGANIZATION;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}

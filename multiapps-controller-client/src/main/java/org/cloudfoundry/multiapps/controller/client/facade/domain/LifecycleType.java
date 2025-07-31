package org.cloudfoundry.multiapps.controller.client.facade.domain;

public enum LifecycleType {

    BUILDPACK, DOCKER, KPACK, CNB;

    public String toString() {
        return this.name()
                   .toLowerCase();
    }

}

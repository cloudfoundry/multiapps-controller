package com.sap.cloud.lm.sl.cf.core.k8s.model;

import io.fabric8.kubernetes.api.model.LocalObjectReference;

public class ServiceBindingSpec {

    private LocalObjectReference instanceRef;
    private String secretName;

    public LocalObjectReference getInstanceRef() {
        return instanceRef;
    }

    public String getSecretName() {
        return secretName;
    }

    public void setInstanceRef(LocalObjectReference instanceRef) {
        this.instanceRef = instanceRef;
    }

    public void setSecretName(String secretName) {
        this.secretName = secretName;
    }

}

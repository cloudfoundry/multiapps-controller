package com.sap.cloud.lm.sl.cf.core.k8s.model;

import java.io.Serializable;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMeta;

public class ServiceInstance implements Serializable, HasMetadata {

    private static final long serialVersionUID = 1L;

    private String apiVersion = "servicecatalog.k8s.io/v1beta1";
    private String kind = "ServiceInstance";
    private ObjectMeta metadata;
    private ServiceInstanceSpec spec;

    @Override
    public String getApiVersion() {
        return apiVersion;
    }

    @Override
    public String getKind() {
        return kind;
    }

    @Override
    public ObjectMeta getMetadata() {
        return metadata;
    }

    public ServiceInstanceSpec getSpec() {
        return spec;
    }

    @Override
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    @Override
    public void setMetadata(ObjectMeta metadata) {
        this.metadata = metadata;
    }

    public void setSpec(ServiceInstanceSpec spec) {
        this.spec = spec;
    }

}

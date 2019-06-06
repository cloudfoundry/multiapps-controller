package com.sap.cloud.lm.sl.cf.core.model;

public class ServiceMtaMetadata {

    private DeployedMtaMetadata mtaMetadata;
    private DeployedMtaResource deployedMtaResource;

    public ServiceMtaMetadata() {
    }

    private ServiceMtaMetadata(Builder builder) {
        this.mtaMetadata = builder.mtaMetadata;
        this.deployedMtaResource = builder.deployedMtaResource;
    }

    public DeployedMtaMetadata getMtaMetadata() {
        return mtaMetadata;
    }

    public void setMtaMetadata(DeployedMtaMetadata mtaMetadata) {
        this.mtaMetadata = mtaMetadata;
    }

    public DeployedMtaResource getDeployedMtaResource() {
        return deployedMtaResource;
    }

    public void setDeployedMtaResource(DeployedMtaResource deployedMtaResource) {
        this.deployedMtaResource = deployedMtaResource;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private DeployedMtaMetadata mtaMetadata;
        private DeployedMtaResource deployedMtaResource;

        private Builder() {
        }

        public Builder withMtaMetadata(DeployedMtaMetadata mtaMetadata) {
            this.mtaMetadata = mtaMetadata;
            return this;
        }

        public Builder withDeployedMtaResource(DeployedMtaResource deployedMtaResource) {
            this.deployedMtaResource = deployedMtaResource;
            return this;
        }

        public ServiceMtaMetadata build() {
            return new ServiceMtaMetadata(this);
        }
    }
}

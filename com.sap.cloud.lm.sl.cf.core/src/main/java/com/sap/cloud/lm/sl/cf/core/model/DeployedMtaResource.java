package com.sap.cloud.lm.sl.cf.core.model;

import java.util.Date;

public class DeployedMtaResource {
    private String resourceName;
    private String serviceName;
    private Date createdOn;
    private Date updatedOn;


    private DeployedMtaResource(Builder builder) {
        this.resourceName = builder.resourceName;
        this.serviceName = builder.serviceName;
        this.createdOn = builder.createdOn;
        this.updatedOn = builder.updatedOn;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public Date getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(Date updatedOn) {
        this.updatedOn = updatedOn;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String resourceName;
        private String serviceName;
        private Date createdOn;
        private Date updatedOn;

        private Builder() {
        }

        public Builder withResourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public Builder withServiceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder withCreatedOn(Date createdOn) {
            this.createdOn = createdOn;
            return this;
        }

        public Builder withUpdatedOn(Date updatedOn) {
            this.updatedOn = updatedOn;
            return this;
        }

        public DeployedMtaResource build() {
            return new DeployedMtaResource(this);
        }
    }
}

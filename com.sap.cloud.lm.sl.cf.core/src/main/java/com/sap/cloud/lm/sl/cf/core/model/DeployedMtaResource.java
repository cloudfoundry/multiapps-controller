package com.sap.cloud.lm.sl.cf.core.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DeployedMtaResource {
    private String resourceName;
    private String serviceName;
    private Date createdOn;
    private Date updatedOn;
    private List<DeployedMtaModule> modules;
    private Map<String, Object> serviceInstanceParameters;

    public DeployedMtaResource() {
    }

    private DeployedMtaResource(Builder builder) {
        this.resourceName = builder.resourceName;
        this.serviceName = builder.serviceName;
        this.createdOn = builder.createdOn;
        this.updatedOn = builder.updatedOn;
        this.modules = builder.modules;
        this.serviceInstanceParameters = builder.serviceInstanceParameters;
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

    public List<DeployedMtaModule> getModules() {
        return modules;
    }

    public void setModules(List<DeployedMtaModule> modules) {
        this.modules = modules;
    }

    public Map<String, Object> getServiceInstanceParameters() {
        return serviceInstanceParameters;
    }

    public void setServiceInstanceParameters(Map<String, Object> serviceInstanceParameters) {
        this.serviceInstanceParameters = serviceInstanceParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeployedMtaResource that = (DeployedMtaResource) o;
        return Objects.equals(resourceName, that.resourceName) &&
                Objects.equals(serviceName, that.serviceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceName, serviceName);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String resourceName;
        private String serviceName;
        private Date createdOn;
        private Date updatedOn;
        private List<DeployedMtaModule> modules = new ArrayList();
        private Map<String, Object> serviceInstanceParameters = new HashMap<>();

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

        public Builder withModules(List<DeployedMtaModule> modules) {
            this.modules = modules;
            return this;
        }

        public Builder withServiceInstanceParameters(Map<String, Object> serviceInstanceParameters) {
            this.serviceInstanceParameters = serviceInstanceParameters;
            return this;
        }

        public DeployedMtaResource build() {
            return new DeployedMtaResource(this);
        }
    }
}

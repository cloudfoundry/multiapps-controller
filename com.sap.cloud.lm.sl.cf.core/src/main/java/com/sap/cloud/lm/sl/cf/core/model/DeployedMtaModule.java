package com.sap.cloud.lm.sl.cf.core.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DeployedMtaModule {

    private String moduleName;
    private String appName;
    private Date createdOn;
    private Date updatedOn;
    private List<DeployedMtaResource> resources;
    private List<String> providedDependencyNames;
    private List<String> uris;

    private DeployedMtaModule(Builder builder) {
        this.moduleName = builder.moduleName;
        this.appName = builder.appName;
        this.createdOn = builder.createdOn;
        this.updatedOn = builder.updatedOn;
        this.resources = builder.resources;
        this.providedDependencyNames = builder.providedDependencyNames;
        this.uris = builder.uris;
    }

    public DeployedMtaModule() {
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getAppName() {
        return appName;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public Date getUpdatedOn() {
        return updatedOn;
    }

    public List<DeployedMtaResource> getResources() {
        return resources;
    }

    public List<String> getProvidedDependencyNames() {
        return providedDependencyNames;
    }

    public List<String> getUris() {
        return uris;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public void setUpdatedOn(Date updatedOn) {
        this.updatedOn = updatedOn;
    }

    public void setResources(List<DeployedMtaResource> resources) {
        this.resources = resources;
    }

    public void setProvidedDependencyNames(List<String> providedDependencyNames) {
        this.providedDependencyNames = providedDependencyNames;
    }

    public void setUris(List<String> uris) {
        this.uris = uris;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String moduleName;
        private String appName;
        private Date createdOn;
        private Date updatedOn;
        private List<DeployedMtaResource> resources = new ArrayList<>();
        private List<String> providedDependencyNames = new ArrayList<>();
        private List<String> uris = new ArrayList<>();

        private Builder() {
        }

        public Builder withModuleName(String moduleName) {
            this.moduleName = moduleName;
            return this;
        }

        public Builder withAppName(String appName) {
            this.appName = appName;
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

        public Builder withServices(List<DeployedMtaResource> resources) {
            this.resources = resources;
            return this;
        }

        public Builder withProvidedDependencyNames(List<String> providedDependencyNames) {
            this.providedDependencyNames = providedDependencyNames;
            return this;
        }

        public Builder withUris(List<String> uris) {
            this.uris = uris;
            return this;
        }

        public DeployedMtaModule build() {
            return new DeployedMtaModule(this);
        }
    }
}

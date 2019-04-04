package com.sap.cloud.lm.sl.cf.core.model;

import java.util.List;
import java.util.Map;

import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaResource;
import javax.annotation.Generated;
import java.util.Collections;

public class ServiceMtaMetadata {
    
    private DeployedMtaMetadata mtaMetadata;
    private DeployedMtaResource deployedMtaResource;
    private List<String> boundApps;
    private Map<String, String> appsCredentials;

    private ServiceMtaMetadata(Builder builder) {
        this.mtaMetadata = builder.mtaMetadata;
        this.deployedMtaResource = builder.deployedMtaResource;
        this.boundApps = builder.boundApps;
        this.appsCredentials = builder.appsCredentials;
    }
    
    public DeployedMtaResource getDeployedMtaResource() {
        return deployedMtaResource;
    }
    public void setDeployedMtaResource(DeployedMtaResource deployedMtaResource) {
        this.deployedMtaResource = deployedMtaResource;
    }
    public List<String> getBoundApps() {
        return boundApps;
    }
    public void setBoundApps(List<String> boundApps) {
        this.boundApps = boundApps;
    }
    public Map<String, String> getAppsCredentials() {
        return appsCredentials;
    }
    public void setAppsCredentials(Map<String, String> appsCredentials) {
        this.appsCredentials = appsCredentials;
    }
    public DeployedMtaMetadata getMtaMetadata() {
        return mtaMetadata;
    }
    public void setMtaMetadata(DeployedMtaMetadata mtaMetadata) {
        this.mtaMetadata = mtaMetadata;
    }

    
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private DeployedMtaMetadata mtaMetadata;
        private DeployedMtaResource deployedMtaResource;
        private List<String> boundApps = Collections.emptyList();
        private Map<String, String> appsCredentials = Collections.emptyMap();

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

        public Builder withBoundApps(List<String> boundApps) {
            this.boundApps = boundApps;
            return this;
        }

        public Builder withAppsCredentials(Map<String, String> appsCredentials) {
            this.appsCredentials = appsCredentials;
            return this;
        }

        public ServiceMtaMetadata build() {
            return new ServiceMtaMetadata(this);
        }
    }
    
    
}

package com.sap.cloud.lm.sl.cf.core.model;

import java.util.ArrayList;
import java.util.List;

public class DeployedMta {

    private MtaMetadata metadata;
    private List<DeployedMtaModule> modules = new ArrayList<>();
    private List<DeployedMtaResource> resources = new ArrayList<>();

    private DeployedMta(Builder builder) {
        this.metadata = builder.metadata;
        this.modules = builder.modules;
        this.resources = builder.resources;
    }

    public DeployedMta() {
    }

    public DeployedMta(MtaMetadata metadata, List<DeployedMtaModule> modules, List<DeployedMtaResource> resources) {
        this.metadata = metadata;
        this.modules = modules;
        this.resources = resources;
    }

    public MtaMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(MtaMetadata metadata) {
        this.metadata = metadata;
    }

    public List<DeployedMtaModule> getModules() {
        return modules;
    }

    public void setModules(List<DeployedMtaModule> modules) {
        this.modules = modules;
    }

    public List<DeployedMtaResource> getResources() {
        return resources;
    }

    public void setResources(List<DeployedMtaResource> resources) {
        this.resources = resources;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DeployedMta other = (DeployedMta) obj;
        if (metadata == null) {
            if (other.metadata != null)
                return false;
        } else if (!metadata.equals(other.metadata))
            return false;
        return true;
    }

    public DeployedMtaModule findDeployedModule(String moduleName) {
        return getModules().stream()
                           .filter(module -> module.getModuleName()
                                                   .equalsIgnoreCase(moduleName))
                           .findFirst()
                           .orElse(null);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private MtaMetadata metadata;
        private List<DeployedMtaModule> modules = new ArrayList<>();
        private List<DeployedMtaResource> resources = new ArrayList<>();

        private Builder() {
        }

        public Builder withMetadata(MtaMetadata metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder withModules(List<DeployedMtaModule> modules) {
            this.modules = modules;
            return this;
        }

        public Builder withResources(List<DeployedMtaResource> resources) {
            this.resources = resources;
            return this;
        }

        public DeployedMta build() {
            return new DeployedMta(this);
        }
    }

}

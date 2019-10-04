package com.sap.cloud.lm.sl.cf.core.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DeployedMta {

    private DeployedMtaMetadata metadata;
    private List<DeployedMtaModule> modules;
    private Set<String> services;

    public DeployedMta() {
    }

    public DeployedMta(DeployedMtaMetadata metadata, List<DeployedMtaModule> modules, Set<String> services) {
        this.metadata = metadata;
        this.modules = modules;
        this.services = services;
    }

    public DeployedMtaMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(DeployedMtaMetadata metadata) {
        this.metadata = metadata;
    }

    public List<DeployedMtaModule> getModules() {
        return modules;
    }

    public void setModules(List<DeployedMtaModule> modules) {
        this.modules = modules;
    }

    public Set<String> getServices() {
        return services;
    }

    public void setServices(Set<String> services) {
        this.services = services;
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null) {
            return false;
        }
        if (getClass() != object.getClass()) {
            return false;
        }
        DeployedMta other = (DeployedMta) object;
        return Objects.equals(metadata, other.metadata);
    }

    public DeployedMtaModule findDeployedModule(String moduleName) {
        return getModules().stream()
                           .filter(module -> module.getModuleName()
                                                   .equalsIgnoreCase(moduleName))
                           .findFirst()
                           .orElse(null);
    }

}

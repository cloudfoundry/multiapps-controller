package com.sap.cloud.lm.sl.cf.core.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication.ProductizationState;

public class DeployedMta {

    private DeployedMtaMetadata metadata;
    private List<DeployedMtaApplication> applications;
    private Set<String> services;

    public DeployedMta() {
    }

    public DeployedMta(DeployedMtaMetadata metadata, List<DeployedMtaApplication> applications, Set<String> services) {
        this.metadata = metadata;
        this.applications = applications;
        this.services = services;
    }

    public DeployedMtaMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(DeployedMtaMetadata metadata) {
        this.metadata = metadata;
    }

    public List<DeployedMtaApplication> getApplications() {
        return applications;
    }

    public void setApplications(List<DeployedMtaApplication> applications) {
        this.applications = applications;
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

    public DeployedMtaApplication findApplication(String moduleName, DeployedMtaApplication.ProductizationState state) {
        return getApplications().stream()
                           .filter(deployedApplication -> doesDeployedApplicationMatchModuleName(deployedApplication, moduleName))
                           .filter(deployedApplication -> doesDeployedApplicationMatchProductizationState(deployedApplication, state))
                           .findFirst()
                           .orElse(null);
    }

    private boolean doesDeployedApplicationMatchModuleName(DeployedMtaApplication deployedApplication, String moduleName) {
        return deployedApplication.getModuleName()
                                  .equalsIgnoreCase(moduleName);
    }

    private boolean doesDeployedApplicationMatchProductizationState(DeployedMtaApplication deployedApplication, ProductizationState state) {
        return deployedApplication.getProductizationState() == state;
    }

}

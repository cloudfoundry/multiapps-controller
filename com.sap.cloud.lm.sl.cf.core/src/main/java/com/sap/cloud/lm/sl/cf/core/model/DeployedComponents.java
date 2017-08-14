package com.sap.cloud.lm.sl.cf.core.model;

import java.util.List;

public class DeployedComponents {

    private List<DeployedMta> mtas;
    private List<String> standaloneApps;

    public DeployedComponents() {
    }

    public DeployedComponents(List<DeployedMta> mtas, List<String> standaloneApps) {
        this.mtas = mtas;
        this.standaloneApps = standaloneApps;
    }

    public List<DeployedMta> getMtas() {
        return mtas;
    }

    public void setMtas(List<DeployedMta> mtas) {
        this.mtas = mtas;
    }

    public List<String> getStandaloneApps() {
        return standaloneApps;
    }

    public void setStandaloneApps(List<String> standaloneApps) {
        this.standaloneApps = standaloneApps;
    }

    public DeployedMta findDeployedMta(String mtaId) {
        return getMtas().stream().filter(mta -> mta.getMetadata().getId().equalsIgnoreCase(mtaId)).findFirst().orElse(null);
    }

}

package com.sap.cloud.lm.sl.cf.core.k8s.model;

public class ServiceInstanceSpec {

    private String clusterServiceClassExternalName;
    private String clusterServicePlanExternalName;

    public String getClusterServiceClassExternalName() {
        return clusterServiceClassExternalName;
    }

    public String getClusterServicePlanExternalName() {
        return clusterServicePlanExternalName;
    }

    public void setClusterServiceClassExternalName(String clusterServiceClassExternalName) {
        this.clusterServiceClassExternalName = clusterServiceClassExternalName;
    }

    public void setClusterServicePlanExternalName(String clusterServicePlanExternalName) {
        this.clusterServicePlanExternalName = clusterServicePlanExternalName;
    }

}

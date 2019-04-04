package com.sap.cloud.lm.sl.cf.core.model;

import java.util.List;

/**
 * MTA metadata information associated with an application;
 */
public class ApplicationMtaMetadata {

    private final String moduleName;
    private final DeployedMtaMetadata mtaMetadata;
    private final List<DeployedMtaResource> services;
    private final List<String> providedDependencyNames;

    public ApplicationMtaMetadata(DeployedMtaMetadata mtaMetadata, List<DeployedMtaResource> services, String moduleName,
        List<String> providedDependencyNames) {
        this.moduleName = moduleName;
        this.mtaMetadata = mtaMetadata;
        this.services = services;
        this.providedDependencyNames = providedDependencyNames;
    }

    public String getModuleName() {
        return moduleName;
    }

    public DeployedMtaMetadata getMtaMetadata() {
        return mtaMetadata;
    }

    public List<DeployedMtaResource> getServices() {
        return services;
    }

    public List<String> getProvidedDependencyNames() {
        return providedDependencyNames;
    }

}

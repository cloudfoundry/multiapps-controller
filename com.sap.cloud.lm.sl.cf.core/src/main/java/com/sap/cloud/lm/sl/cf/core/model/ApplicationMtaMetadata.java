package com.sap.cloud.lm.sl.cf.core.model;

import java.util.List;

/**
 * MTA metadata information associated with an application;
 */
public class ApplicationMtaMetadata {

    private final DeployedMtaMetadata mtaMetadata;
    private final List<String> services;
    private final List<String> sharedServices;
    private final String moduleName;
    private final List<String> providedDependencyNames;

    public ApplicationMtaMetadata(DeployedMtaMetadata mtaMetadata, List<String> services, List<String> sharedServices, String moduleName,
        List<String> providedDependencyNames) {
        this.mtaMetadata = mtaMetadata;
        this.services = services;
        this.sharedServices = sharedServices;
        this.moduleName = moduleName;
        this.providedDependencyNames = providedDependencyNames;
    }

    public DeployedMtaMetadata getMtaMetadata() {
        return mtaMetadata;
    }

    public List<String> getServices() {
        return services;
    }

    public List<String> getSharedServices() {
        return sharedServices;
    }

    public String getModuleName() {
        return moduleName;
    }

    public List<String> getProvidedDependencyNames() {
        return providedDependencyNames;
    }

}

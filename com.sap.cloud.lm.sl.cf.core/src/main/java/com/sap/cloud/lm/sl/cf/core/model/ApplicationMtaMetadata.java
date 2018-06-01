package com.sap.cloud.lm.sl.cf.core.model;

import java.util.List;
import java.util.Map;

/**
 * MTA metadata information associated with an application;
 */
public class ApplicationMtaMetadata {

    private final DeployedMtaMetadata mtaMetadata;
    private final List<String> services;
    private final List<String> sharedServices;
    private final String moduleName;
    private final List<String> providedDependencyNames;
    private final Map<String, Object> deployAttributes;

    public ApplicationMtaMetadata(DeployedMtaMetadata mtaMetadata, List<String> services, List<String> sharedServices, String moduleName,
        List<String> providedDependencyNames, Map<String, Object> deployAttributes) {
        this.mtaMetadata = mtaMetadata;
        this.services = services;
        this.sharedServices = sharedServices;
        this.moduleName = moduleName;
        this.providedDependencyNames = providedDependencyNames;
        this.deployAttributes = deployAttributes;
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

    public Map<String, Object> getDeployAttributes() {
        return deployAttributes;
    }

}

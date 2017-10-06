package com.sap.cloud.lm.sl.cf.core.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class DeployedMtaModule {

    private String moduleName;
    private String appName;
    private Date createdOn;
    private Date updatedOn;
    private List<String> services;
    private List<String> providedDependencyNames;
    private Map<String, Object> deployAttributes;
    private List<String> uris;

    public DeployedMtaModule() {
    }

    public DeployedMtaModule(String moduleName, String appName, Date createdOn, Date updatedOn, List<String> services,
        List<String> providedDependencyNames, Map<String, Object> deployAttributes, List<String> uris) {
        this.moduleName = moduleName;
        this.appName = appName;
        this.createdOn = createdOn;
        this.updatedOn = updatedOn;
        this.services = services;
        this.providedDependencyNames = providedDependencyNames;
        this.deployAttributes = deployAttributes;
        this.uris = uris;
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

    public List<String> getServices() {
        return services;
    }

    public List<String> getProvidedDependencyNames() {
        return providedDependencyNames;
    }

    public Map<String, Object> getDeployAttributes() {
        return deployAttributes;
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

    public void setServices(List<String> services) {
        this.services = services;
    }

    public void setProvidedDependencyNames(List<String> providedDependencyNames) {
        this.providedDependencyNames = providedDependencyNames;
    }

    public void setDeployAttributes(Map<String, Object> deployAttributes) {
        this.deployAttributes = deployAttributes;
    }

    public void setUris(List<String> uris) {
        this.uris = uris;
    }

}

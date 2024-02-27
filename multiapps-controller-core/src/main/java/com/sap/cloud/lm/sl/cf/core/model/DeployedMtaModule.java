package com.sap.cloud.lm.sl.cf.core.model;

import java.util.Date;
import java.util.List;

public class DeployedMtaModule {

    private String moduleName;
    private String appName;
    private Date createdOn;
    private Date updatedOn;
    private List<String> services;
    private List<String> providedDependencyNames;
    private List<String> uris;

    public DeployedMtaModule() {
    }

    public DeployedMtaModule(String moduleName, String appName, Date createdOn, Date updatedOn, List<String> services,
                             List<String> providedDependencyNames, List<String> uris) {
        this.moduleName = moduleName;
        this.appName = appName;
        this.createdOn = createdOn;
        this.updatedOn = updatedOn;
        this.services = services;
        this.providedDependencyNames = providedDependencyNames;
        this.uris = uris;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public Date getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(Date updatedOn) {
        this.updatedOn = updatedOn;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }

    public List<String> getProvidedDependencyNames() {
        return providedDependencyNames;
    }

    public void setProvidedDependencyNames(List<String> providedDependencyNames) {
        this.providedDependencyNames = providedDependencyNames;
    }

    public List<String> getUris() {
        return uris;
    }

    public void setUris(List<String> uris) {
        this.uris = uris;
    }

}

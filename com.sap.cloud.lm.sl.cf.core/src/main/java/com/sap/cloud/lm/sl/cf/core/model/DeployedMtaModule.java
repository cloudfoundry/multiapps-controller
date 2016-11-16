package com.sap.cloud.lm.sl.cf.core.model;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlAccessorType(value = javax.xml.bind.annotation.XmlAccessType.FIELD)
@XmlRootElement(name = "module")
public class DeployedMtaModule {

    private String moduleName;
    private String appName;
    @XmlTransient
    private Date createdOn;
    @XmlTransient
    private Date updatedOn;
    @XmlElementWrapper(name = "services")
    @XmlElement(name = "service")
    private List<String> services;
    @XmlElementWrapper(name = "providedDependencies")
    @XmlElement(name = "providedDependency")
    private List<String> providedDependencyNames;

    public DeployedMtaModule() {
        // Required by JAXB
    }

    public DeployedMtaModule(String moduleName, String appName, Date createdOn, Date updatedOn, List<String> services,
        List<String> providedDependencyNames) {
        this.moduleName = moduleName;
        this.appName = appName;
        this.createdOn = createdOn;
        this.updatedOn = updatedOn;
        this.services = services;
        this.providedDependencyNames = providedDependencyNames;
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

}

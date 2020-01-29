package com.sap.cloud.lm.sl.cf.core.dto.serialization;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;

@XmlAccessorType(XmlAccessType.FIELD)
public class DeployedMtaApplicationDto {

    private String moduleName;
    private String appName;
    @XmlElementWrapper(name = "services")
    @XmlElement(name = "service")
    private List<String> services;
    @XmlElementWrapper(name = "providedDependencies")
    @XmlElement(name = "providedDependency")
    private List<String> providedDependencyNames;

    protected DeployedMtaApplicationDto() {
        // Required by JAXB
    }

    public DeployedMtaApplicationDto(DeployedMtaApplication deployedApplication) {
        this.moduleName = deployedApplication.getModuleName();
        this.appName = deployedApplication.getAppName();
        this.services = deployedApplication.getServices();
        this.providedDependencyNames = deployedApplication.getProvidedDependencyNames();
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getAppName() {
        return appName;
    }

    public List<String> getServices() {
        return services;
    }

    public List<String> getProvidedDependencyNames() {
        return providedDependencyNames;
    }

    public DeployedMtaApplication toDeployedMtaApplication() {
        DeployedMtaApplication result = new DeployedMtaApplication();
        result.setModuleName(moduleName);
        result.setAppName(appName);
        result.setServices(services);
        result.setProvidedDependencyNames(providedDependencyNames);
        return result;
    }

}

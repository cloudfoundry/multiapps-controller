package com.sap.cloud.lm.sl.cf.core.dto.serialization;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;

@XmlAccessorType(XmlAccessType.FIELD)
public class DeployedMtaModuleDto {

    private String moduleName;
    private String appName;
    @XmlElementWrapper(name = "services")
    @XmlElement(name = "service")
    private List<String> services;
    @XmlElementWrapper(name = "providedDependencies")
    @XmlElement(name = "providedDependency")
    private List<String> providedDependencyNames;

    protected DeployedMtaModuleDto() {
        // Required by JAXB
    }

    public DeployedMtaModuleDto(DeployedMtaModule module) {
        this.moduleName = module.getModuleName();
        this.appName = module.getAppName();
        this.services = module.getServices();
        this.providedDependencyNames = module.getProvidedDependencyNames();
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

    public DeployedMtaModule toDeployedMtaModule() {
        DeployedMtaModule result = new DeployedMtaModule();
        result.setModuleName(moduleName);
        result.setAppName(appName);
        result.setServices(services);
        result.setProvidedDependencyNames(providedDependencyNames);
        return result;
    }

}

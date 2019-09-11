package com.sap.cloud.lm.sl.cf.core.dto.serialization;

import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaResource;

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
        this.services = extractDeployedResourceServiceNames(module);
        this.providedDependencyNames = module.getProvidedDependencyNames();
    }

    private List<String> extractDeployedResourceServiceNames(DeployedMtaModule module) {
        return module.getResources()
                              .stream()
                              .map(DeployedMtaResource::getServiceName)
                              .collect(Collectors.toList());
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
        List<DeployedMtaResource> moduleServices = services.stream()
                                                           .map(serviceName -> DeployedMtaResource.builder()
                                                                                                  .withServiceName(serviceName)
                                                                                                  .build())
                                                           .collect(Collectors.toList());
        DeployedMtaModule result = DeployedMtaModule.builder()
                                                    .withModuleName(moduleName)
                                                    .withAppName(appName)
                                                    .withResources(moduleServices)
                                                    .withProvidedDependencyNames(providedDependencyNames)
                                                    .build();
        return result;
    }

}

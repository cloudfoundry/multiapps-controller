package com.sap.cloud.lm.sl.cf.core.dto.serialization;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaResource;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "mta")
public class DeployedMtaDto {

    private DeployedMtaMetadataDto metadata;

    @XmlElementWrapper(name = "modules")
    @XmlElement(name = "module")
    private List<DeployedMtaModuleDto> modules;

    @XmlElementWrapper(name = "services")
    @XmlElement(name = "service")
    private Set<String> services;

    protected DeployedMtaDto() {
        // Required by JAXB
    }

    public DeployedMtaDto(DeployedMta mta) {
        this.metadata = new DeployedMtaMetadataDto(mta.getMetadata());
        this.modules = toDtos(mta.getModules());
        this.services = mta.getServices()
                           .stream()
                           .map(s -> s.getServiceName())
                           .collect(Collectors.toSet());
    }

    private static List<DeployedMtaModuleDto> toDtos(List<DeployedMtaModule> modules) {
        return modules.stream()
                      .map(DeployedMtaModuleDto::new)
                      .collect(Collectors.toList());
    }

    private static List<DeployedMtaModule> toDeployedMtaModules(List<DeployedMtaModuleDto> modules) {
        return modules.stream()
                      .map(DeployedMtaModuleDto::toDeployedMtaModule)
                      .collect(Collectors.toList());
    }

    public DeployedMtaMetadataDto getMetadata() {
        return metadata;
    }

    public List<DeployedMtaModuleDto> getModules() {
        return modules;
    }

    public Set<String> getServices() {
        return services;
    }

    public DeployedMta toDeployedMta() {
        DeployedMta result = new DeployedMta();
        result.setMetadata(metadata.toDeployedMtaMetadata());
        result.setModules(toDeployedMtaModules(modules));
        result.setServices(services.stream()
                                   .map(n -> DeployedMtaResource.builder()
                                                                .withServiceName(n)
                                                                .build())
                                   .collect(Collectors.toList()));
        return result;
    }

}

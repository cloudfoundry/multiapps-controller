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
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "mta")
public class DeployedMtaDto {

    private DeployedMtaMetadataDto metadata;

    @XmlElementWrapper(name = "applications")
    @XmlElement(name = "application")
    private List<DeployedMtaApplicationDto> applications;

    @XmlElementWrapper(name = "services")
    @XmlElement(name = "service")
    private Set<String> services;

    protected DeployedMtaDto() {
        // Required by JAXB
    }

    public DeployedMtaDto(DeployedMta mta) {
        this.metadata = new DeployedMtaMetadataDto(mta.getMetadata());
        this.applications = toDtos(mta.getApplications());
        this.services = mta.getServices();
    }

    private static List<DeployedMtaApplicationDto> toDtos(List<DeployedMtaApplication> applications) {
        return applications.stream()
                           .map(DeployedMtaApplicationDto::new)
                           .collect(Collectors.toList());
    }

    private static List<DeployedMtaApplication> toDeployedMtaApplications(List<DeployedMtaApplicationDto> applications) {
        return applications.stream()
                           .map(DeployedMtaApplicationDto::toDeployedMtaApplication)
                           .collect(Collectors.toList());
    }

    public DeployedMtaMetadataDto getMetadata() {
        return metadata;
    }

    public List<DeployedMtaApplicationDto> getApplications() {
        return applications;
    }

    public Set<String> getServices() {
        return services;
    }

    public DeployedMta toDeployedMta() {
        DeployedMta result = new DeployedMta();
        result.setMetadata(metadata.toDeployedMtaMetadata());
        result.setApplications(toDeployedMtaApplications(applications));
        result.setServices(services);
        return result;
    }

}

package com.sap.cloud.lm.sl.cf.core.dto.serialization;

import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "components")
public class DeployedComponentsDto {

    @XmlElementWrapper(name = "mtas")
    @XmlElement(name = "mta")
    private List<DeployedMtaDto> mtas;

    protected DeployedComponentsDto() {
        // Required by JAXB
    }

    public DeployedComponentsDto(List<DeployedMta> mtas) {
        this.mtas = toDtos(mtas);
    }

    public static List<DeployedMta> toDeployedMtas(List<DeployedMtaDto> mtas) {
        return mtas.stream()
                   .map(DeployedMtaDto::toDeployedMta)
                   .collect(Collectors.toList());
    }

    private static List<DeployedMtaDto> toDtos(List<DeployedMta> mtas) {
        return mtas.stream()
                   .map(DeployedMtaDto::new)
                   .collect(Collectors.toList());
    }

    public List<DeployedMtaDto> getMtas() {
        return mtas;
    }
}

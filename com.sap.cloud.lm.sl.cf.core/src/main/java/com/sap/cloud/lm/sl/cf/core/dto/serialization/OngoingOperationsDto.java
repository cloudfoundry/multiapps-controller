package com.sap.cloud.lm.sl.cf.core.dto.serialization;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ongoing-operations")
@XmlAccessorType(XmlAccessType.FIELD)
public class OngoingOperationsDto {

    @XmlElement(name = "ongoing-operation")
    private List<OngoingOperationDto> ongoingOperations;

    protected OngoingOperationsDto() {
        // Required by JAXB
    }

    public OngoingOperationsDto(List<OngoingOperationDto> ongoingOperations) {
        this.ongoingOperations = ongoingOperations;
    }

    public List<OngoingOperationDto> getOngoingOperations() {
        return ongoingOperations;
    }

}

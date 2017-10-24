package com.sap.cloud.lm.sl.cf.core.dto.serialization;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "operations")
@XmlAccessorType(XmlAccessType.FIELD)
public class OperationsDto {

    @XmlElement(name = "operation")
    private List<OperationDto> operations;

    protected OperationsDto() {
        // Required by JAXB
    }

    public OperationsDto(List<OperationDto> ongoingOperations) {
        this.operations = ongoingOperations;
    }

    public List<OperationDto> getOngoingOperations() {
        return operations;
    }

}

package com.sap.cloud.lm.sl.cf.core.dto.serialization;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ongoing-operation")
@XmlAccessorType(XmlAccessType.FIELD)
public class OngoingOperationDto {

    @XmlElement(name = "process-id")
    private String processId;

    @XmlElement(name = "process-type")
    private String processType;

    @XmlElement(name = "started-at")
    private String startedAt;

    @XmlElement(name = "space-id")
    private String spaceId;

    @XmlElement(name = "mta-id")
    private String mtaId;

    @XmlElement(name = "user")
    private String user;

    @XmlElement(name = "acquired-lock")
    private boolean acquiredLock;

    @XmlElement(name = "state")
    private String state;

    protected OngoingOperationDto() {
        // Required by JAXB
    }

    public OngoingOperationDto(String processId, String processType, String startedAt, String spaceId, String mtaId, String user,
        boolean acquiredLock, String state) {
        this.processId = processId;
        this.processType = processType;
        this.startedAt = startedAt;
        this.spaceId = spaceId;
        this.mtaId = mtaId;
        this.user = user;
        this.acquiredLock = acquiredLock;
        this.state = state;
    }

    public String getProcessId() {
        return processId;
    }

    public String getProcessType() {
        return processType;
    }

    public String getStartedAt() {
        return startedAt;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public String getMtaId() {
        return mtaId;
    }

    public String getUser() {
        return user;
    }

    public boolean hasAcquiredLock() {
        return acquiredLock;
    }

    public String getState() {
        return state;
    }

}

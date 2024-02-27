package com.sap.cloud.lm.sl.cf.core.dto.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "operation")
@NamedQueries({ @NamedQuery(name = "find_all", query = "SELECT o FROM OperationDto o ORDER BY o.startedAt"),
    // TODO: Replace this named query by building it dynamically with JPA's criteria API (requires JPA 2.1).
    @NamedQuery(name = "remove_expired_in_final_state", query = "DELETE FROM OperationDto o WHERE o.finalState IS NOT NULL AND o.startedAt < :expirationTime") })
public class OperationDto {

    @Id
    @Column(name = "process_id")
    private String processId;
    @Column(name = "process_type")
    private String processType;
    @Column(name = "started_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startedAt;
    @Column(name = "ended_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date endedAt;
    @Column(name = "space_id")
    private String spaceId;
    @Column(name = "mta_id")
    private String mtaId;
    @Column(name = "userx")
    private String user;
    @Column(name = "acquired_lock")
    private boolean acquiredLock;
    @Column(name = "final_state")
    private String finalState;

    protected OperationDto() {
        // Required by JPA
    }

    public OperationDto(String processId, String processType, Date startedAt, Date endedAt, String spaceId, String mtaId, String user,
                        boolean acquiredLock, String finalState) {
        this.processId = processId;
        this.processType = processType;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.spaceId = spaceId;
        this.mtaId = mtaId;
        this.user = user;
        this.acquiredLock = acquiredLock;
        this.finalState = finalState;
    }

    public String getProcessId() {
        return processId;
    }

    public String getProcessType() {
        return processType;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public Date getEndedAt() {
        return endedAt;
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

    public String getFinalState() {
        return finalState;
    }

    public static class AttributeNames {

        public static final String PROCESS_ID = "processId";
        public static final String PROCESS_TYPE = "processType";
        public static final String STARTED_AT = "startedAt";
        public static final String ENDED_AT = "endedAt";
        public static final String SPACE_ID = "spaceId";
        public static final String MTA_ID = "mtaId";
        public static final String USER = "user";
        public static final String ACQUIRED_LOCK = "acquiredLock";
        public static final String FINAL_STATE = "finalState";

    }

}

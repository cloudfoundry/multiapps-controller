package org.cloudfoundry.multiapps.controller.persistence.dto;

import java.util.Date;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "operation")
@Cacheable(false)
public class OperationDto implements DtoWithPrimaryKey<String> {

    public static class AttributeNames {

        private AttributeNames() {
        }

        public static final String PROCESS_ID = "processId";
        public static final String PROCESS_TYPE = "processType";
        public static final String STARTED_AT = "startedAt";
        public static final String ENDED_AT = "endedAt";
        public static final String SPACE_ID = "spaceId";
        public static final String MTA_ID = "mtaId";
        public static final String NAMESPACE = "namespace";
        public static final String USER = "user";
        public static final String ACQUIRED_LOCK = "acquiredLock";
        public static final String CURRENT_STATE = "currentState";

    }

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

    @Column(name = "namespace")
    private String namespace;

    @Column(name = "userx")
    private String user;

    @Column(name = "acquired_lock")
    private boolean acquiredLock;

    @Column(name = "current_state")
    private String currentState;

    protected OperationDto() {
        // Required by JPA
    }

    private OperationDto(String processId, String processType, Date startedAt, Date endedAt, String spaceId, String mtaId, String namespace, String user,
                         boolean acquiredLock, String currentState) {
        this.processId = processId;
        this.processType = processType;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.spaceId = spaceId;
        this.mtaId = mtaId;
        this.namespace = namespace;
        this.user = user;
        this.acquiredLock = acquiredLock;
        this.currentState = currentState;
    }

    @Override
    public String getPrimaryKey() {
        return getProcessId();
    }

    @Override
    public void setPrimaryKey(String processId) {
        this.processId = processId;
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

    public String getNamespace() {
        return namespace;
    }

    public String getUser() {
        return user;
    }

    public boolean hasAcquiredLock() {
        return acquiredLock;
    }

    public String getCurrentState() {
        return currentState;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String processId;
        private String processType;
        private Date startedAt;
        private Date endedAt;
        private String spaceId;
        private String mtaId;
        private String namespace;
        private String user;
        private boolean acquiredLock;
        private String currentState;

        public Builder processId(String processId) {
            this.processId = processId;
            return this;
        }

        public Builder processType(String processType) {
            this.processType = processType;
            return this;
        }

        public Builder startedAt(Date startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder endedAt(Date endedAt) {
            this.endedAt = endedAt;
            return this;
        }

        public Builder spaceId(String spaceId) {
            this.spaceId = spaceId;
            return this;
        }

        public Builder mtaId(String mtaId) {
            this.mtaId = mtaId;
            return this;
        }

        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

        public Builder acquiredLock(boolean acquiredLock) {
            this.acquiredLock = acquiredLock;
            return this;
        }

        public Builder currentState(String state) {
            this.currentState = state;
            return this;
        }

        public OperationDto build() {
            return new OperationDto(processId, processType, startedAt, endedAt, spaceId, mtaId, namespace, user, acquiredLock, currentState);
        }
    }
}

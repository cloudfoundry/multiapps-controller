package org.cloudfoundry.multiapps.controller.persistence.dto;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Table(name = "application_shutdown")
@SequenceGenerator(name = "application_shutdown_sequence", sequenceName = "application_shutdown_sequence", allocationSize = 1)
public class ApplicationShutdownDto implements DtoWithPrimaryKey<String> {

    public static class AttributeNames {
        private AttributeNames() {
        }

        public static final String ID = "id";
        public static final String APPLICATION_ID = "applicationId";
        public static final String APPLICATION_INSTANCE_INDEX = "applicationInstanceIndex";
        public static final String SHUTDOWN_STATUS = "shutdownStatus";
        public static final String STARTED_AT = "startedAt";
    }

    @Id
    @Column(name = "id", nullable = false, unique = true)
    private String id;

    @Column(name = "application_instance_index", nullable = false, unique = true)
    private int applicationInstanceIndex;

    @Column(name = "application_id", nullable = false)
    private String applicationId;

    @Column(name = "shutdown_status", nullable = false)
    private String shutdownStatus;

    @Column(name = "started_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date startedAt;

    public ApplicationShutdownDto() {
        // Required by JPA
    }

    public ApplicationShutdownDto(String id, String applicationId, int applicationInstanceIndex, String shutdownStatus, Date startedAt) {
        this.id = id;
        this.applicationId = applicationId;
        this.applicationInstanceIndex = applicationInstanceIndex;
        this.shutdownStatus = shutdownStatus;
        this.startedAt = startedAt;
    }

    @Override
    public String getPrimaryKey() {
        return id;
    }

    @Override
    public void setPrimaryKey(String id) {
        this.id = id;
    }

    public String getАpplicationId() {
        return applicationId;
    }

    public int getАpplicationIndex() {
        return applicationInstanceIndex;
    }

    public String getShutdownStatus() {
        return shutdownStatus;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    @Override
    public String toString() {
        return "ApplicationShutdownDto{" +
            "id='" + id + '\'' +
            ", applicationId='" + applicationId + '\'' +
            ", shutdownStatus='" + shutdownStatus + '\'' +
            ", applicationInstanceIndex='" + applicationInstanceIndex + '\'' +
            ", startedAt='" + startedAt + '\'' +
            '}';
    }
}

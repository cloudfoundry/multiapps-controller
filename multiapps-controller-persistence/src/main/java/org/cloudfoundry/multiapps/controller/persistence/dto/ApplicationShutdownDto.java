package org.cloudfoundry.multiapps.controller.persistence.dto;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata;

@Entity
@Table(name = PersistenceMetadata.TableNames.APPLICATION_SHUTDOWN_TABLE)
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
    @Column(name = PersistenceMetadata.TableColumnNames.APPLICATION_SHUTDOWN_ID, nullable = false, unique = true)
    private String id;

    @Column(name = PersistenceMetadata.TableColumnNames.APPLICATION_SHUTDOWN_APPLICATION_INSTANCE_INDEX, nullable = false, unique = true)
    private int applicationInstanceIndex;

    @Column(name = PersistenceMetadata.TableColumnNames.APPLICATION_SHUTDOWN_APPLICATION_ID, nullable = false)
    private String applicationId;

    @Column(name = PersistenceMetadata.TableColumnNames.APPLICATION_SHUTDOWN_SHUTDOWN_STATUS, nullable = false)
    private ApplicationShutdown.Status shutdownStatus;

    @Column(name = PersistenceMetadata.TableColumnNames.APPLICATION_SHUTDOWN_STARTED_AT, nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date startedAt;

    public ApplicationShutdownDto() {
        // Required by JPA
    }

    public ApplicationShutdownDto(String id, String applicationId, int applicationInstanceIndex, ApplicationShutdown.Status shutdownStatus,
                                  Date startedAt) {
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

    public ApplicationShutdown.Status getShutdownStatus() {
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

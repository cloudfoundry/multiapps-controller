package org.cloudfoundry.multiapps.controller.persistence.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = "application_shutdown")
@SequenceGenerator(name = "application_shutdown_sequence", sequenceName = "application_shutdown_sequence", allocationSize = 1)
public class ApplicationShutdownDto implements DtoWithPrimaryKey<String> {

    public static class AttributeNames {
        private AttributeNames() {
        }

        public static final String APPLICATION_ID = "applicationId";
        public static final String APPLICATION_INSTANCE_INDEX = "applicationInstanceIndex";
        public static final String SHUTDOWN_STATUS = "shutdownStatus";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, unique = true)
    private String id;

    @Column(name = "application_instance_index", nullable = false, unique = true)
    private int applicationInstanceIndex;

    @Column(name = "application_id", nullable = false)
    private String applicationId;

    @Column(name = "shutdown_status", nullable = false)
    private String shutdownStatus;

    public ApplicationShutdownDto() {
        // Required by JPA
    }

    public ApplicationShutdownDto(String applicationId, int applicationInstanceIndex, String shutdownStatus) {
        this.applicationId = applicationId;
        this.applicationInstanceIndex = applicationInstanceIndex;
        this.shutdownStatus = shutdownStatus;
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

    @Override
    public String toString() {
        return "ApplicationShutdownDto{" +
            "id='" + id + '\'' +
            ", applicationId='" + applicationId + '\'' +
            ", shutdownStatus='" + shutdownStatus + '\'' +
            ", applicationInstanceIndex='" + applicationInstanceIndex + '\'' +
            '}';
    }
}

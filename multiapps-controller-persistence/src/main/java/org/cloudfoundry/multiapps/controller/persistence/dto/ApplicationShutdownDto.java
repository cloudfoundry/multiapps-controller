package org.cloudfoundry.multiapps.controller.persistence.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata;

@Entity
@Table(name = PersistenceMetadata.TableNames.APPLICATION_SHUTDOWN_TABLE)
@SequenceGenerator(name = PersistenceMetadata.SequenceNames.APPLICATION_SHUTDOWN_SEQUENCE, sequenceName = PersistenceMetadata.SequenceNames.APPLICATION_SHUTDOWN_SEQUENCE, allocationSize = 1)
public class ApplicationShutdownDto implements DtoWithPrimaryKey<String> {

    public static class AttributeNames {
        private AttributeNames() {
        }

        public static final String APPLICATION_INSTANCE_ID = "applicationInstanceId";
        public static final String APPLICATION_ID = "applicationId";
        public static final String APPLICATION_INSTANCE_INDEX = "applicationInstanceIndex";
    }

    @Id
    @Column(name = PersistenceMetadata.TableColumnNames.APPLICATION_INSTANCE_ID, nullable = false, unique = true)
    private String applicationInstanceId;

    @Column(name = PersistenceMetadata.TableColumnNames.APPLICATION_ID, nullable = false, unique = true)
    private String applicationId;

    @Column(name = PersistenceMetadata.TableColumnNames.APPLICATION_INSTANCE_INDEX, nullable = false, unique = true)
    private int applicationInstanceIndex;

    public ApplicationShutdownDto() {
        // Required by JPA
    }

    public ApplicationShutdownDto(String applicationId, String applicationInstanceId, int applicationInstanceIndex) {
        this.applicationId = applicationId;
        this.applicationInstanceId = applicationInstanceId;
        this.applicationInstanceIndex = applicationInstanceIndex;
    }

    @Override
    public String getPrimaryKey() {
        return applicationInstanceId;
    }

    @Override
    public void setPrimaryKey(String applicationInstanceId) {
        this.applicationInstanceId = applicationInstanceId;
    }

    public String getАpplicationId() {
        return applicationId;
    }

    public void setАpplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public int getАpplicationIndex() {
        return applicationInstanceIndex;
    }

    public void setАpplicationIndex(int applicationInstanceIndex) {
        this.applicationInstanceIndex = applicationInstanceIndex;
    }

    @Override
    public String toString() {
        return "AsyncUploadJobDto{" +
            "id='" + applicationInstanceId + '\'' +
            ", applicationId='" + applicationId + '\'' +
            ", applicationInstanceIndex='" + applicationInstanceIndex + '\'' +
            '}';
    }
}

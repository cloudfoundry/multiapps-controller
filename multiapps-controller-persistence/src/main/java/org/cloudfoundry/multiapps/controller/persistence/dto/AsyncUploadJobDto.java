package org.cloudfoundry.multiapps.controller.persistence.dto;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata;

@Entity
@Table(name = PersistenceMetadata.TableNames.ASYNC_UPLOAD_JOB_TABLE)
public class AsyncUploadJobDto implements DtoWithPrimaryKey<String> {

    public static class AttributeNames {

        private AttributeNames() {
        }

        public static final String ID = "id";
        public static final String USER = "mtaUser";
        public static final String STATE = "state";
        public static final String URL = "url";
        public static final String ADDED_AT = "addedAt";
        public static final String STARTED_AT = "startedAt";
        public static final String FINISHED_AT = "finishedAt";
        public static final String NAMESPACE = "namespace";
        public static final String SPACE_GUID = "spaceGuid";
        public static final String MTA_ID = "mtaId";
        public static final String FILE_ID = "fileId";
        public static final String ERROR = "error";
        public static final String INSTANCE_INDEX = "instanceIndex";
    }

    @Id
    @Column(name = PersistenceMetadata.TableColumnNames.ASYNC_UPLOAD_JOB_ID)
    private String id;

    @Column(name = PersistenceMetadata.TableColumnNames.ASYNC_UPLOAD_JOB_USER, nullable = false)
    private String mtaUser;

    @Column(name = PersistenceMetadata.TableColumnNames.ASYNC_UPLOAD_JOB_STATE, nullable = false)
    private String state;

    @Column(name = PersistenceMetadata.TableColumnNames.ASYNC_UPLOAD_JOB_URL, nullable = false)
    private String url;

    @Column(name = PersistenceMetadata.TableColumnNames.ASYNC_UPLOAD_JOB_ADDED_AT)
    private LocalDateTime addedAt;

    @Column(name = PersistenceMetadata.TableColumnNames.ASYNC_UPLOAD_JOB_STARTED_AT)
    private LocalDateTime startedAt;

    @Column(name = PersistenceMetadata.TableColumnNames.ASYNC_UPLOAD_JOB_FINISHED_AT)
    private LocalDateTime finishedAt;

    @Column(name = PersistenceMetadata.TableColumnNames.ASYNC_UPLOAD_JOB_NAMESPACE)
    private String namespace;

    @Column(name = PersistenceMetadata.TableColumnNames.ASYNC_UPLOAD_JOB_SPACE_GUID, nullable = false)
    private String spaceGuid;

    @Column(name = PersistenceMetadata.TableColumnNames.ASYNC_UPLOAD_JOB_MTA_ID)
    private String mtaId;

    @Column(name = PersistenceMetadata.TableColumnNames.ASYNC_UPLOAD_JOB_FILE_ID)
    private String fileId;

    @Column(name = PersistenceMetadata.TableColumnNames.ASYNC_UPLOAD_JOB_ERROR)
    private String error;

    @Column(name = PersistenceMetadata.TableColumnNames.ASYNC_UPLOAD_JOB_INSTANCE_INDEX, nullable = false)
    private Integer instanceIndex;

    public AsyncUploadJobDto() {
        // Required by JPA
    }

    public AsyncUploadJobDto(String id, String mtaUser, String state, String url, LocalDateTime addedAt, LocalDateTime startedAt,
                             LocalDateTime finishedAt, String namespace, String spaceGuid, String mtaId, String fileId, String error,
                             Integer instanceIndex) {
        this.id = id;
        this.mtaUser = mtaUser;
        this.state = state;
        this.url = url;
        this.addedAt = addedAt;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.namespace = namespace;
        this.spaceGuid = spaceGuid;
        this.mtaId = mtaId;
        this.fileId = fileId;
        this.error = error;
        this.instanceIndex = instanceIndex;
    }

    @Override
    public String getPrimaryKey() {
        return id;
    }

    @Override
    public void setPrimaryKey(String primaryKey) {
        this.id = primaryKey;
    }

    public String getMtaUser() {
        return mtaUser;
    }

    public void setMtaUser(String mtaUser) {
        this.mtaUser = mtaUser;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getSpaceGuid() {
        return spaceGuid;
    }

    public void setSpaceGuid(String spaceGuid) {
        this.spaceGuid = spaceGuid;
    }

    public String getMtaId() {
        return mtaId;
    }

    public void setMtaId(String mtaId) {
        this.mtaId = mtaId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Integer getInstanceIndex() {
        return instanceIndex;
    }

    public void setInstanceIndex(Integer instanceIndex) {
        this.instanceIndex = instanceIndex;
    }

    @Override
    public String toString() {
        return "AsyncUploadJobDto{" +
                "id='" + id + '\'' +
                ", mtaUser='" + mtaUser + '\'' +
                ", state='" + state + '\'' +
                ", url='" + url + '\'' +
                ", addedAt=" + addedAt +
                ", startedAt=" + startedAt +
                ", finishedAt=" + finishedAt +
                ", namespace='" + namespace + '\'' +
                ", spaceGuid='" + spaceGuid + '\'' +
                ", mtaId='" + mtaId + '\'' +
                ", fileId='" + fileId + '\'' +
                ", error='" + error + '\'' +
                ", instanceIndex=" + instanceIndex +
                '}';
    }
}

package org.cloudfoundry.multiapps.controller.persistence.dto;

import java.time.LocalDateTime;

import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata.SequenceNames;
import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata.TableColumnNames;
import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata.TableNames;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity
@Table(name = TableNames.BACKUP_DESCRIPTOR_TABLE)
@SequenceGenerator(name = SequenceNames.BACKUP_DESCRIPTOR_SEQUENCE, sequenceName = SequenceNames.BACKUP_DESCRIPTOR_SEQUENCE, allocationSize = 1)
public class BackupDescriptorDto implements DtoWithPrimaryKey<Long> {

    public static class AttributeNames {
        private AttributeNames() {
        }

        public static final String ID = "id";
        public static final String MTA_ID = "mtaId";
        public static final String SPACE_ID = "spaceId";
        public static final String NAMESPACE = "namespace";
        public static final String MTA_VERSION = "mtaVersion";
        public static final String TIMESTAMP = "timestamp";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = SequenceNames.BACKUP_DESCRIPTOR_SEQUENCE)
    @Column(name = TableColumnNames.BACKUP_DESCRIPTOR_ID)
    private long id;

    @Column(name = TableColumnNames.BACKUP_DESCRIPTOR_DESCRIPTOR, nullable = false)
    @Lob
    private byte[] descriptor;

    @Column(name = TableColumnNames.BACKUP_DESCRIPTOR_MTA_ID, nullable = false)
    private String mtaId;

    @Column(name = TableColumnNames.BACKUP_DESCRIPTOR_MTA_VERSION, nullable = false)
    private String mtaVersion;

    @Column(name = TableColumnNames.BACKUP_DESCRIPTOR_SPACE_ID, nullable = false)
    private String spaceId;

    @Column(name = TableColumnNames.BACKUP_DESCRIPTOR_NAMESPACE, nullable = true)
    private String namespace;

    @Column(name = TableColumnNames.BACKUP_DESCRIPTOR_TIMESTAMP, nullable = false)
    private LocalDateTime timestamp;

    protected BackupDescriptorDto() {
        // Required by JPA
    }

    public BackupDescriptorDto(long id, byte[] descriptor, String mtaId, String mtaVersion, String spaceId, String namespace,
                               LocalDateTime timestamp) {
        this.id = id;
        this.descriptor = descriptor;
        this.mtaId = mtaId;
        this.mtaVersion = mtaVersion;
        this.spaceId = spaceId;
        this.namespace = namespace;
        this.timestamp = timestamp;
    }

    @Override
    public Long getPrimaryKey() {
        return id;
    }

    @Override
    public void setPrimaryKey(Long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public byte[] getdescriptor() {
        return descriptor;
    }

    public String getMtaId() {
        return mtaId;
    }

    public String getMtaVersion() {
        return mtaVersion;
    }

    public String getSpaceId() {
        return spaceId;
    }

    public String getNamespace() {
        return namespace;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

}

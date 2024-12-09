package org.cloudfoundry.multiapps.controller.persistence.dto;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata.SequenceNames;
import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata.TableColumnNames;
import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata.TableNames;

@Entity
@Table(name = TableNames.PRESERVED_DESCRIPTOR_TABLE)
@SequenceGenerator(name = SequenceNames.DESCRIPTOR_PRESERVER_SEQUENCE, sequenceName = SequenceNames.DESCRIPTOR_PRESERVER_SEQUENCE, allocationSize = 1)
public class PreservedDescriptorDto implements DtoWithPrimaryKey<Long> {

    public static class AttributeNames {
        private AttributeNames() {
        }

        public static final String ID = "id";
        public static final String MTA_ID = "mtaId";
        public static final String SPACE_ID = "spaceId";
        public static final String NAMESPACE = "namespace";
        public static final String CHECKSUM = "checksum";
        public static final String TIMESTAMP = "timestamp";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = SequenceNames.DESCRIPTOR_PRESERVER_SEQUENCE)
    @Column(name = TableColumnNames.PRESERVED_DESCRIPTOR_ID)
    private long id;

    @Column(name = TableColumnNames.PRESERVED_DESCRIPTOR_DESCRIPTOR, nullable = false)
    @Lob
    private byte[] descriptor;

    @Column(name = TableColumnNames.PRESERVED_DESCRIPTOR_MTA_ID, nullable = false)
    private String mtaId;

    @Column(name = TableColumnNames.PRESERVED_DESCRIPTOR_MTA_VERSION, nullable = false)
    private String mtaVersion;

    @Column(name = TableColumnNames.PRESERVED_DESCRIPTOR_SPACE_ID, nullable = false)
    private String spaceId;

    @Column(name = TableColumnNames.PRESERVED_DESCRIPTOR_NAMESPACE, nullable = true)
    private String namespace;

    @Column(name = TableColumnNames.PRESERVED_DESCRIPTOR_CHECKSUM, nullable = false)
    private String checksum;

    @Column(name = TableColumnNames.PRESERVED_DESCRIPTOR_TIMESTAMP, nullable = false)
    private LocalDateTime timestamp;

    protected PreservedDescriptorDto() {
        // Required by JPA
    }

    public PreservedDescriptorDto(long id, byte[] descriptor, String mtaId, String mtaVersion, String spaceId, String namespace,
                                  String checksum, LocalDateTime timestamp) {
        this.id = id;
        this.descriptor = descriptor;
        this.mtaId = mtaId;
        this.mtaVersion = mtaVersion;
        this.spaceId = spaceId;
        this.namespace = namespace;
        this.checksum = checksum;
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

    public String getChecksum() {
        return checksum;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

}

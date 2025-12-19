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
import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata;

@Entity
@Table(name = PersistenceMetadata.TableNames.SECRET_TOKEN)
@SequenceGenerator(name = PersistenceMetadata.SequenceNames.SECRET_TOKEN_SEQUENCE, sequenceName = PersistenceMetadata.SequenceNames.SECRET_TOKEN_SEQUENCE, allocationSize = 1)
public class SecretTokenDto implements DtoWithPrimaryKey<Long> {

    public static class AttributeNames {
        private AttributeNames() {
        }

        public static final String ID = "id";
        public static final String PROCESS_INSTANCE_ID = "process_instance_id";
        public static final String VARIABLE_NAME = "variable_name";
        public static final String CONTENT = "content";
        public static final String TIMESTAMP = "timestamp";
        public static final String KEY_ID = "key_id";
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = PersistenceMetadata.SequenceNames.SECRET_TOKEN_SEQUENCE)
    @Column(name = PersistenceMetadata.TableColumnNames.SECRET_TOKEN_ID)
    private long id;

    @Column(name = PersistenceMetadata.TableColumnNames.SECRET_TOKEN_PROCESS_INSTANCE_ID, nullable = false)
    private String processInstanceId;

    @Column(name = PersistenceMetadata.TableColumnNames.SECRET_TOKEN_VARIABLE_NAME, nullable = false)
    private String variableName;

    @Column(name = PersistenceMetadata.TableColumnNames.SECRET_TOKEN_CONTENT, nullable = false)
    @Lob
    private byte[] content;

    @Column(name = PersistenceMetadata.TableColumnNames.SECRET_TOKEN_TIMESTAMP, nullable = false)
    private LocalDateTime timestamp;

    @Column(name = PersistenceMetadata.TableColumnNames.SECRET_TOKEN_KEY_ID, nullable = false)
    private String keyId;

    protected SecretTokenDto() {
        //Required by JPA
    }

    public SecretTokenDto(long id, String processInstanceId, String variableName, byte[] content, LocalDateTime timestamp, String keyId) {
        this.id = id;
        this.processInstanceId = processInstanceId;
        this.variableName = variableName;
        this.content = content;
        this.timestamp = timestamp;
        this.keyId = keyId;
    }

    @Override
    public Long getPrimaryKey() {
        return this.id;
    }

    @Override
    public void setPrimaryKey(Long id) {
        this.id = id;
    }

    public long getId() {
        return this.id;
    }

    public String getProcessInstanceId() {
        return this.processInstanceId;
    }

    public String getVariableName() {
        return this.variableName;
    }

    public byte[] getContent() {
        return this.content;
    }

    public LocalDateTime getTimestamp() {
        return this.timestamp;
    }

    public String getKeyId() {
        return this.keyId;
    }

}

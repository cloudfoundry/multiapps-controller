package org.cloudfoundry.multiapps.controller.persistence.dto;

import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = PersistenceMetadata.TableNames.LOCK_OWNERS_TABLE)
@SequenceGenerator(name = PersistenceMetadata.SequenceNames.LOCK_OWNERS_SEQUENCE, sequenceName = PersistenceMetadata.SequenceNames.LOCK_OWNERS_SEQUENCE, allocationSize = 1)
public class LockOwnerDto implements DtoWithPrimaryKey<Long> {

    public static class AttributeNames {

        private AttributeNames() {
        }

        public static final String ID = "id";
        public static final String LOCK_OWNER = "lockOwner";
        public static final String TIMESTAMP = "timestamp";

    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = PersistenceMetadata.SequenceNames.LOCK_OWNERS_SEQUENCE)
    @Column(name = PersistenceMetadata.TableColumnNames.LOCK_OWNER_ID)
    private long id;

    @Column(name = PersistenceMetadata.TableColumnNames.LOCK_OWNER_LOCK_OWNER, nullable = false)
    private String lockOwner;

    @Column(name = PersistenceMetadata.TableColumnNames.LOCK_OWNER_TIMESTAMP, nullable = false)
    private LocalDateTime timestamp;

    public LockOwnerDto() {
        // Required by JPA
    }

    public LockOwnerDto(long id, String lockOwner, LocalDateTime timestamp) {
        this.id = id;
        this.lockOwner = lockOwner;
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

    public String getLockOwner() {
        return lockOwner;
    }

    public void setLockOwner(String lockOwner) {
        this.lockOwner = lockOwner;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "LockOwnerDto{" + "lockOwner='" + lockOwner + '\'' + ", timestamp=" + timestamp + '}';
    }
}

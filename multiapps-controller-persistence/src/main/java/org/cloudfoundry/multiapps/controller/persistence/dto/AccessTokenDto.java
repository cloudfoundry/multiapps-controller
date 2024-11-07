package org.cloudfoundry.multiapps.controller.persistence.dto;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import org.cloudfoundry.multiapps.controller.persistence.model.PersistenceMetadata;

@Entity
@Table(name = PersistenceMetadata.TableNames.ACCESS_TOKEN_TABLE)
@SequenceGenerator(name = PersistenceMetadata.SequenceNames.ACCESS_TOKEN_SEQUENCE, sequenceName = PersistenceMetadata.SequenceNames.ACCESS_TOKEN_SEQUENCE, allocationSize = 1)
public class AccessTokenDto implements DtoWithPrimaryKey<Long> {

    public static class AttributeNames {

        private AttributeNames() {
        }

        public static final String ID = "id";
        public static final String VALUE = "value";
        public static final String USERNAME = "username";
        public static final String EXPIRES_AT = "expiresAt";

    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = PersistenceMetadata.SequenceNames.ACCESS_TOKEN_SEQUENCE)
    @Column(name = PersistenceMetadata.TableColumnNames.ACCESS_TOKEN_ID)
    private long id;

    @Column(name = PersistenceMetadata.TableColumnNames.ACCESS_TOKEN_VALUE, nullable = false)
    private byte[] value;

    @Column(name = PersistenceMetadata.TableColumnNames.ACCESS_TOKEN_USERNAME, nullable = false)
    private String username;

    @Column(name = PersistenceMetadata.TableColumnNames.ACCESS_TOKEN_EXPIRES_AT, nullable = false)
    private LocalDateTime expiresAt;

    public AccessTokenDto() {
        // Required by JPA
    }

    public AccessTokenDto(long id, byte[] value, String username, LocalDateTime expiresAt) {
        this.id = id;
        this.value = value;
        this.username = username;
        this.expiresAt = expiresAt;
    }

    @Override
    public Long getPrimaryKey() {
        return id;
    }

    @Override
    public void setPrimaryKey(Long id) {
        this.id = id;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}

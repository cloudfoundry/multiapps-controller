package com.sap.cloud.lm.sl.cf.core.dto.persistence;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import com.sap.cloud.lm.sl.cf.core.model.AccessToken;
import com.sap.cloud.lm.sl.cf.core.model.PersistenceMetadata;

@Entity
@Table(name = PersistenceMetadata.TableNames.ACCESS_TOKEN_TABLE)
@SequenceGenerator(name = PersistenceMetadata.SequenceNames.ACCESS_TOKEN_SEQUENCE, sequenceName = PersistenceMetadata.SequenceNames.ACCESS_TOKEN_SEQUENCE, allocationSize = 1)
public class AccessTokenDto implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = PersistenceMetadata.SequenceNames.ACCESS_TOKEN_SEQUENCE)
    @Column(name = PersistenceMetadata.TableColumnNames.ACCESS_TOKEN_ID)
    private long id;

    @Column(name = PersistenceMetadata.TableColumnNames.ACCESS_TOKEN_VALUE, nullable = false)
    private byte[] value;

    @Column(name = PersistenceMetadata.TableColumnNames.EXCHANGED_ACCESS_TOKEN_VALUE)
    private byte[] exchangedTokenValue;

    @Column(name = PersistenceMetadata.TableColumnNames.CLIENT_ID, nullable = false)
    private String clientId;

    @Column(name = PersistenceMetadata.TableColumnNames.ACCESS_TOKEN_USERNAME, nullable = false)
    private String username;

    @Column(name = PersistenceMetadata.TableColumnNames.ACCESS_TOKEN_EXPIRES_AT, nullable = false)
    private LocalDateTime expiresAt;

    public AccessTokenDto() {
        // Required by JPA
    }

    public AccessTokenDto(AccessToken accessToken) {
        this.id = accessToken.getId();
        this.value = accessToken.getValue();
        this.exchangedTokenValue = accessToken.getExchangedTokenValue();
        this.clientId = accessToken.getClientId();
        this.username = accessToken.getUsername();
        this.expiresAt = accessToken.getExpiresAt();
    }

    public AccessTokenDto(byte[] value, byte[] exchangedTokenValue, String clientId, String username, LocalDateTime expiresAt) {
        this.value = value;
        this.exchangedTokenValue = exchangedTokenValue;
        this.clientId = clientId;
        this.username = username;
        this.expiresAt = expiresAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
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

    public byte[] getExchangedTokenValue() {
        return exchangedTokenValue;
    }

    public void setExchangedTokenValue(byte[] exchangedTokenValue) {
        this.exchangedTokenValue = exchangedTokenValue;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public AccessToken toAccessToken() {
        return new AccessToken(getId(), getValue(), getExchangedTokenValue(), getClientId(), getUsername(), getExpiresAt());
    }

    public static class AttributeNames {

        public static final String ID = "id";
        public static final String VALUE = "value";
        public static final String EXCHANGED_TOKEN_VALUE = "exchangedTokenValue";
        public static final String CLIENT_ID = "clientId";
        public static final String USERNAME = "username";
        public static final String EXPIRES_AT = "expiresAt";
        private AttributeNames() {
        }

    }

}

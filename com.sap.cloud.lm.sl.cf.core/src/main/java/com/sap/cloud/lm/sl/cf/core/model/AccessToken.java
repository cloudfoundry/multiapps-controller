package com.sap.cloud.lm.sl.cf.core.model;

import java.time.LocalDateTime;

public class AccessToken {

    private long id;
    private byte[] value;
    private byte[] exchangedTokenValue;
    private String clientId;
    private String username;
    private LocalDateTime expiresAt;

    public AccessToken() {
    }

    public AccessToken(byte[] value, byte[] exchangedTokenValue, String clientId, String username, LocalDateTime expiresAt) {
        this.value = value;
        this.exchangedTokenValue = exchangedTokenValue;
        this.clientId = clientId;
        this.username = username;
        this.expiresAt = expiresAt;
    }

    public AccessToken(long id, byte[] value, byte[] exchangedTokenValue, String clientId, String username, LocalDateTime expiresAt) {
        this.id = id;
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
}

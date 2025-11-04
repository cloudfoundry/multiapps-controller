package org.cloudfoundry.multiapps.controller.persistence.dto;

import java.util.Map;

public class DatabaseServiceKey {

    private final Map<String, Object> credentials;

    public DatabaseServiceKey(Map<String, Object> credentials) {
        this.credentials = credentials;
    }

    public String getUsername() {
        return getValueFromCredentials("username");
    }

    public String getPassword() {
        return getValueFromCredentials("password");
    }

    public String getJdbcUri() {
        String uri = getValueFromCredentials("uri");
        uri = uri.replaceFirst(getUsername() + ":" + getPassword() + "@", "")
                 .replaceFirst("^postgres://", "jdbc:postgresql://");
        uri = uri + "?user=" + getUsername() + "&password=" + getPassword();
        return uri;
    }

    private String getValueFromCredentials(String key) {
        return getCredentialsObject().get(key)
                                     .toString();
    }

    private Map<String, Object> getCredentialsObject() {
        return (Map<String, Object>) credentials.get("credentials");
    }
}

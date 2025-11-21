package org.cloudfoundry.multiapps.controller.persistence.dto;

import java.text.MessageFormat;
import java.util.Map;

public class DatabaseServiceKey {

    private final Map<String, Object> credentials;
    private static final String JDBC_URI_TEMPLATE = "jdbc:postgresql://{0}:{1}/{2}?user={3}&password={4}";

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
        return MessageFormat.format(JDBC_URI_TEMPLATE, getHostname(), getPort(), getDbName(), getUsername(), getPassword());
    }

    private String getHostname() {
        return getValueFromCredentials("hostname");
    }

    private String getPort() {
        return getValueFromCredentials("port");
    }

    private String getDbName() {
        return getValueFromCredentials("dbname");
    }

    private String getValueFromCredentials(String key) {
        return getCredentialsObject().get(key)
                                     .toString();
    }

    private Map<String, Object> getCredentialsObject() {
        return (Map<String, Object>) credentials.get("credentials");
    }
}

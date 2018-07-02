package com.sap.cloud.lm.sl.cf.core.k8s.model;

public class DockerRegistryCredentials {

    private final String username;
    private final String password;

    public DockerRegistryCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

}

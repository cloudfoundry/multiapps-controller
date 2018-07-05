package com.sap.cloud.lm.sl.cf.core.k8s.model;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class DockerConfiguration {

    private final Map<String, DockerRegistryCredentials> registryCredentials;

    public DockerConfiguration(Map<String, DockerRegistryCredentials> registryCredentials) {
        this.registryCredentials = registryCredentials;
    }

    public Map<String, DockerRegistryCredentials> getRegistryCredentials() {
        return registryCredentials;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("auths", getRegistryCredentialsAsMap());
        return result;
    }

    private Map<String, Object> getRegistryCredentialsAsMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String registry : registryCredentials.keySet()) {
            result.put(registry, getRegistryCredentialsAsMap(registry));
        }
        return result;
    }

    private Map<String, Object> getRegistryCredentialsAsMap(String registry) {
        DockerRegistryCredentials credentials = registryCredentials.get(registry);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("auth", encode(credentials));
        return result;
    }

    private String encode(DockerRegistryCredentials credentials) {
        String concatenatedCredentials = String.format("%s:%s", credentials.getUsername(), credentials.getPassword());
        byte[] encodedCredentials = Base64.getEncoder()
            .encode(concatenatedCredentials.getBytes(StandardCharsets.UTF_8));
        return new String(encodedCredentials, StandardCharsets.UTF_8);
    }

    public static class Builder {

        private Map<String, DockerRegistryCredentials> repositoryCredentials = new LinkedHashMap<>();

        public Builder addCredentialsForRepository(String repository, DockerRegistryCredentials credentials) {
            repositoryCredentials.put(repository, credentials);
            return this;
        }

        public DockerConfiguration build() {
            return new DockerConfiguration(repositoryCredentials);
        }

    }

}

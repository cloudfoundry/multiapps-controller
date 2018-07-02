package com.sap.cloud.lm.sl.cf.core.k8s.model;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class DockerConfiguration {

    @SerializedName("auths")
    private final Map<String, DockerRegistryCredentials> repositoryCredentials;

    public DockerConfiguration(Map<String, DockerRegistryCredentials> repositoryCredentials) {
        this.repositoryCredentials = repositoryCredentials;
    }

    public Map<String, DockerRegistryCredentials> getRepositoryCredentials() {
        return repositoryCredentials;
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

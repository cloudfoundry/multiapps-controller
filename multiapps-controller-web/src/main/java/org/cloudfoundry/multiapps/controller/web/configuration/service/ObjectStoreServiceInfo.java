package org.cloudfoundry.multiapps.controller.web.configuration.service;

import org.springframework.cloud.service.BaseServiceInfo;
import org.springframework.cloud.service.ServiceInfo.ServiceLabel;

@ServiceLabel("objectstore")
public class ObjectStoreServiceInfo extends BaseServiceInfo {

    private String provider;
    private String identity;
    private String credential;
    private String container;
    private String endpoint;
    private String region;

    private ObjectStoreServiceInfo(String id, String provider, String identity, String credential, String container, String endpoint, String region) {
        super(id);
        this.provider = provider;
        this.identity = identity;
        this.credential = credential;
        this.container = container;
        this.endpoint = endpoint;
        this.region = region;
    }

    public String getProvider() {
        return provider;
    }

    public String getIdentity() {
        return identity;
    }

    public String getCredential() {
        return credential;
    }

    public String getContainer() {
        return container;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getRegion() {
        return region;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String id;
        private String provider;
        private String identity;
        private String credential;
        private String container;
        private String endpoint;
        private String region;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder provider(String provider) {
            this.provider = provider;
            return this;
        }

        public Builder identity(String identity) {
            this.identity = identity;
            return this;
        }

        public Builder credential(String credential) {
            this.credential = credential;
            return this;
        }

        public Builder container(String container) {
            this.container = container;
            return this;
        }

        public Builder endpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public Builder region(String region) {
            this.region = region;
            return this;
        }

        public ObjectStoreServiceInfo build() {
            return new ObjectStoreServiceInfo(id, provider, identity, credential, container, endpoint, region);
        }
    }
}

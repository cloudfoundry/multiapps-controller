package org.cloudfoundry.multiapps.controller.persistence.jclouds.providers.googlecloudstorage;

import java.net.URI;
import java.util.Properties;

import org.jclouds.providers.ProviderMetadata;
import org.jclouds.providers.internal.BaseProviderMetadata;

import com.google.auto.service.AutoService;

@AutoService(ProviderMetadata.class)
public final class GoogleCloudStorageProviderMetadata extends BaseProviderMetadata {

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder().fromProviderMetadata(this);
    }

    public GoogleCloudStorageProviderMetadata() {
        super(builder());
    }

    public GoogleCloudStorageProviderMetadata(Builder builder) {
        super(builder);
    }

    public static Properties defaultProperties() {
        return new Properties(); // currently all are set in the api metadata class.
    }

    public static final class Builder extends BaseProviderMetadata.Builder {

        private Builder() {
            id("google-cloud-storage-custom").name("Google Cloud Storage")
                                      .apiMetadata(new GoogleCloudStorageApiMetadata())
                                      .homepage(URI.create("https://cloud.google.com/storage"))
                                      .console(URI.create("https://console.developers.google.com/project"))
                                      .defaultProperties(GoogleCloudStorageProviderMetadata.defaultProperties());
        }

        @Override
        public GoogleCloudStorageProviderMetadata build() {
            return new GoogleCloudStorageProviderMetadata(this);
        }

        @Override
        public Builder fromProviderMetadata(ProviderMetadata in) {
            super.fromProviderMetadata(in);
            return this;
        }
    }
}

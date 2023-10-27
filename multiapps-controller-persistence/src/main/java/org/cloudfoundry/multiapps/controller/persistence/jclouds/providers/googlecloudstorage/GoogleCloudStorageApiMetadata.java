package org.cloudfoundry.multiapps.controller.persistence.jclouds.providers.googlecloudstorage;

import static org.jclouds.Constants.PROPERTY_IDEMPOTENT_METHODS;
import static org.jclouds.Constants.PROPERTY_SESSION_INTERVAL;
import static org.jclouds.googlecloudstorage.reference.GoogleCloudStorageConstants.OPERATION_COMPLETE_INTERVAL;
import static org.jclouds.googlecloudstorage.reference.GoogleCloudStorageConstants.OPERATION_COMPLETE_TIMEOUT;
import static org.jclouds.oauth.v2.config.OAuthProperties.AUDIENCE;
import static org.jclouds.oauth.v2.config.OAuthProperties.JWS_ALG;
import static org.jclouds.reflect.Reflection2.typeToken;

import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.cloudfoundry.multiapps.controller.persistence.jclouds.providers.googlecloudstorage.blobstore.config.CustomGoogleCloudStorageBlobStoreContextModule;
import org.jclouds.Constants;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.googlecloud.config.CurrentProject;
import org.jclouds.googlecloudstorage.GoogleCloudStorageApi;
import org.jclouds.googlecloudstorage.config.GoogleCloudStorageHttpApiModule;
import org.jclouds.googlecloudstorage.config.GoogleCloudStorageParserModule;
import org.jclouds.oauth.v2.config.OAuthModule;
import org.jclouds.rest.internal.BaseHttpApiMetadata;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;

public class GoogleCloudStorageApiMetadata extends BaseHttpApiMetadata<GoogleCloudStorageApi> {

    @Override
    public Builder toBuilder() {
        return new Builder().fromApiMetadata(this);
    }

    public GoogleCloudStorageApiMetadata() {
        this(new Builder());
    }

    protected GoogleCloudStorageApiMetadata(Builder builder) {
        super(builder);
    }

    public static Properties defaultProperties() {
        Properties properties = BaseHttpApiMetadata.defaultProperties();
        properties.put("oauth.endpoint", "https://accounts.google.com/o/oauth2/token");
        properties.put(AUDIENCE, "https://accounts.google.com/o/oauth2/token");
        properties.put(JWS_ALG, "RS256");
        properties.put(PROPERTY_SESSION_INTERVAL, 3600);
        properties.put(OPERATION_COMPLETE_INTERVAL, 2000);
        properties.put(OPERATION_COMPLETE_TIMEOUT, 600000);
        properties.setProperty(PROPERTY_IDEMPOTENT_METHODS, "DELETE,GET,HEAD,OPTIONS,POST,PUT");
        // bucket operations have a longer timeout
        properties.setProperty(Constants.PROPERTY_RETRY_DELAY_START, String.valueOf(TimeUnit.SECONDS.toMillis(1)));
        return properties;
    }

    public static class Builder extends BaseHttpApiMetadata.Builder<GoogleCloudStorageApi, Builder> {
        protected Builder() {
            id("google-cloud-storage-custom").name("Google Cloud Storage Api")
                                             .identityName(CurrentProject.ClientEmail.DESCRIPTION)
                                             .credentialName("PEM encoded P12 private key associated with client_email")
                                             .documentation(URI.create("https://developers.google.com/storage/docs/json_api"))
                                             .version("v1")
                                             .defaultEndpoint("https://www.googleapis.com")
                                             .defaultProperties(GoogleCloudStorageApiMetadata.defaultProperties())
                                             .view(typeToken(BlobStoreContext.class))
                                             .defaultModules(ImmutableSet.<Class<? extends Module>> builder()
                                                                         .add(GoogleCloudStorageParserModule.class)
                                                                         .add(OAuthModule.class)
                                                                         .add(GoogleCloudStorageHttpApiModule.class)
                                                                         .add(CustomGoogleCloudStorageBlobStoreContextModule.class)
                                                                         .build());
        }

        @Override
        public GoogleCloudStorageApiMetadata build() {
            return new GoogleCloudStorageApiMetadata(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}

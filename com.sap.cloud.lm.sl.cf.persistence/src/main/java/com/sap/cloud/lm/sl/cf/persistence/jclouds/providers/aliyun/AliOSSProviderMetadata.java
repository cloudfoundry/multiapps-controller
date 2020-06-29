package com.sap.cloud.lm.sl.cf.persistence.jclouds.providers.aliyun;

import com.google.auto.service.AutoService;
import org.jclouds.blobstore.reference.BlobStoreConstants;
import org.jclouds.providers.ProviderMetadata;
import org.jclouds.providers.internal.BaseProviderMetadata;

import java.util.Properties;

@AutoService(ProviderMetadata.class)
public class AliOSSProviderMetadata extends BaseProviderMetadata {

    public AliOSSProviderMetadata() {
        this(builder());
    }

    public AliOSSProviderMetadata(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new AliOSSProviderMetadataBuilder();
    }

    @Override
    public Builder toBuilder() {
        return builder().fromProviderMetadata(this);
    }

    public static Properties defaultProperties() {
        Properties properties = new Properties();
        properties.setProperty(BlobStoreConstants.PROPERTY_BLOBSTORE_DIRECTORY_SUFFIX, BlobStoreConstants.DIRECTORY_SUFFIX_ROOT);
        return properties;
    }

    public static class AliOSSProviderMetadataBuilder extends BaseProviderMetadata.Builder {

        protected AliOSSProviderMetadataBuilder() {
            id(AliOSSApi.API_ID).name(AliOSSApiMetadata.AliOSSConstants.ALI_OSS_API_NAME)
                                .apiMetadata(new AliOSSApiMetadata())
                                .defaultProperties(AliOSSProviderMetadata.defaultProperties());
        }

        @Override
        public ProviderMetadata build() {
            return new AliOSSProviderMetadata(this);
        }

        @Override
        public Builder fromProviderMetadata(ProviderMetadata providerMetadata) {
            super.fromProviderMetadata(providerMetadata);
            return this;
        }
    }
}

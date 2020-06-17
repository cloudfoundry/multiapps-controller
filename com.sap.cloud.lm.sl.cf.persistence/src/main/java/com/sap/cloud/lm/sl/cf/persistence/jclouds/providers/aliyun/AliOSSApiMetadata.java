package com.sap.cloud.lm.sl.cf.persistence.jclouds.providers.aliyun;

import com.sap.cloud.lm.sl.cf.persistence.jclouds.providers.aliyun.blobstore.config.AliOSSBlobStoreContextModule;
import org.jclouds.apis.ApiMetadata;
import org.jclouds.apis.internal.BaseApiMetadata;
import org.jclouds.blobstore.internal.BlobStoreContextImpl;
import org.jclouds.rest.internal.BaseHttpApiMetadata;

import java.net.URI;
import java.util.Properties;

public class AliOSSApiMetadata extends BaseApiMetadata {

    public AliOSSApiMetadata() {
        this(new AliOSSApiMetadataBuilder());
    }

    protected AliOSSApiMetadata(Builder builder) {
        super(builder);
    }

    @Override
    public Builder toBuilder() {
        return new AliOSSApiMetadataBuilder().fromApiMetadata(this);
    }

    public static Properties defaultProperties() {
        return BaseHttpApiMetadata.defaultProperties();
    }

    public static class AliOSSApiMetadataBuilder extends BaseApiMetadata.Builder<AliOSSApiMetadataBuilder> {

        protected AliOSSApiMetadataBuilder() {
            id(AliOSSApi.API_ID).name(AliOSSConstants.ALI_OSS_API_NAME)
                                .identityName(AliOSSConstants.ALI_OSS_API_IDENTITY)
                                .credentialName(AliOSSConstants.ALI_OSS_API_CREDENTIAL)
                                .documentation(URI.create(AliOSSConstants.ALI_OSS_API_DOCUMENTATION_URI))
                                .defaultEndpoint(AliOSSConstants.ALI_OSS_API_DEFAULT_ENDPOINT)
                                .defaultProperties(AliOSSApiMetadata.defaultProperties())
                                .view(BlobStoreContextImpl.class)
                                .defaultModule(AliOSSBlobStoreContextModule.class);
        }

        @Override
        public ApiMetadata build() {
            return new AliOSSApiMetadata(this);
        }

        @Override
        protected AliOSSApiMetadataBuilder self() {
            return this;
        }
    }

    /**
     * Contains constants that are plain metadata, which is not used in business logic.
     */
    public static class AliOSSConstants {
        public static final String ALI_OSS_API_NAME = "AlibabaCloud Object Storage Service";
        private static final String ALI_OSS_API_IDENTITY = "Access Key ID";
        private static final String ALI_OSS_API_CREDENTIAL = "Secret Access Key";
        private static final String ALI_OSS_API_DOCUMENTATION_URI = "https://help.aliyun.com/document_detail/oss/api-reference/abstract.html";
        private static final String ALI_OSS_API_DEFAULT_ENDPOINT = "http://oss.aliyuncs.com";
    }
}

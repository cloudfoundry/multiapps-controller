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
            id(AliOSSApi.API_ID).name("AliCloud Object Storage Service API")
                                .identityName("Access Key ID")
                                .credentialName("Secret Access Key")
                                .documentation(URI.create("https://help.aliyun.com/document_detail/oss/api-reference/abstract.html"))
                                .defaultEndpoint("http://oss.aliyuncs.com")
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
}

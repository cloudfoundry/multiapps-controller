package org.cloudfoundry.multiapps.controller.persistence.jclouds.providers.aliyun.blobstore.config;

import com.google.inject.AbstractModule;

import org.cloudfoundry.multiapps.controller.persistence.jclouds.providers.aliyun.blobstore.AliOSSBlobStore;
import org.cloudfoundry.multiapps.controller.persistence.jclouds.providers.aliyun.blobstore.internal.AliOSSBlobBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.attr.ConsistencyModel;
import org.jclouds.blobstore.domain.BlobBuilder;
import org.jclouds.blobstore.util.BlobUtils;
import org.jclouds.blobstore.util.internal.BlobUtilsImpl;

public class AliOSSBlobStoreContextModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(BlobStore.class).to(AliOSSBlobStore.class);
        bind(BlobBuilder.class).to(AliOSSBlobBuilder.class);
        bind(ConsistencyModel.class).toInstance(ConsistencyModel.STRICT);
        bind(BlobUtils.class).to(BlobUtilsImpl.class);
    }
}

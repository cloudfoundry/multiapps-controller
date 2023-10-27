package org.cloudfoundry.multiapps.controller.persistence.jclouds.providers.googlecloudstorage.blobstore.config;

import org.cloudfoundry.multiapps.controller.persistence.jclouds.providers.googlecloudstorage.blobstore.GoogleCloudStorageBlobStore;
import org.jclouds.blobstore.BlobRequestSigner;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.attr.ConsistencyModel;
import org.jclouds.googlecloudstorage.blobstore.GoogleCloudStorageBlobRequestSigner;
import org.jclouds.googlecloudstorage.blobstore.config.GoogleCloudStorageBlobStoreContextModule;

import com.google.inject.Scopes;

public class CustomGoogleCloudStorageBlobStoreContextModule extends GoogleCloudStorageBlobStoreContextModule {

    @Override
    protected void configure() {
        bind(ConsistencyModel.class).toInstance(ConsistencyModel.EVENTUAL);
        bind(BlobStore.class).to(GoogleCloudStorageBlobStore.class)
                             .in(Scopes.SINGLETON);
        bind(BlobRequestSigner.class).to(GoogleCloudStorageBlobRequestSigner.class);
    }

}

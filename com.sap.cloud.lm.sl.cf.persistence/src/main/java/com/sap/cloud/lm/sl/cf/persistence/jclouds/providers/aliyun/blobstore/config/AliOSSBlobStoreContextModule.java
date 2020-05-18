package com.sap.cloud.lm.sl.cf.persistence.jclouds.providers.aliyun.blobstore.config;

import com.google.inject.AbstractModule;
import com.sap.cloud.lm.sl.cf.persistence.jclouds.providers.aliyun.AliOSSRegion;
import com.sap.cloud.lm.sl.cf.persistence.jclouds.providers.aliyun.blobstore.AliOSSBlobStore;
import com.sap.cloud.lm.sl.cf.persistence.jclouds.providers.aliyun.blobstore.AliOSSLocationsSupplier;
import com.sap.cloud.lm.sl.cf.persistence.jclouds.providers.aliyun.blobstore.AliOSSRegionIdsSupplier;
import com.sap.cloud.lm.sl.cf.persistence.jclouds.providers.aliyun.blobstore.internal.AliOSSBlobBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.attr.ConsistencyModel;
import org.jclouds.blobstore.domain.BlobBuilder;
import org.jclouds.blobstore.util.BlobUtils;
import org.jclouds.blobstore.util.internal.BlobUtilsImpl;
import org.jclouds.location.suppliers.LocationsSupplier;
import org.jclouds.location.suppliers.RegionIdsSupplier;

public class AliOSSBlobStoreContextModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(BlobStore.class).to(AliOSSBlobStore.class);
        bind(BlobBuilder.class).to(AliOSSBlobBuilder.class);
        bind(ConsistencyModel.class).toInstance(ConsistencyModel.EVENTUAL);
        bind(AliOSSRegion.class).toInstance(AliOSSRegion.EU_CENTRAL_1);
        bind(BlobUtils.class).to(BlobUtilsImpl.class);
        bind(LocationsSupplier.class).to(AliOSSLocationsSupplier.class);
        bind(RegionIdsSupplier.class).to(AliOSSRegionIdsSupplier.class);
    }
}

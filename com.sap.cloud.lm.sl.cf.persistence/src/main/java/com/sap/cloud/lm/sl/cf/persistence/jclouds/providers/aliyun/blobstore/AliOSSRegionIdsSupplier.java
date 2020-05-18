package com.sap.cloud.lm.sl.cf.persistence.jclouds.providers.aliyun.blobstore;

import com.sap.cloud.lm.sl.cf.persistence.jclouds.providers.aliyun.AliOSSRegion;
import org.jclouds.location.suppliers.RegionIdsSupplier;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AliOSSRegionIdsSupplier implements RegionIdsSupplier {

    @Override
    public Set<String> get() {
        return Stream.of(AliOSSRegion.values())
                     .map(AliOSSRegion::getRegion)
                     .collect(Collectors.toSet());
    }
}

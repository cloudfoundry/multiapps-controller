package com.sap.cloud.lm.sl.cf.persistence.jclouds.providers.aliyun.blobstore;

import org.jclouds.domain.Location;
import org.jclouds.domain.LocationBuilder;
import org.jclouds.domain.LocationScope;
import org.jclouds.location.suppliers.LocationsSupplier;

import javax.inject.Inject;
import java.util.Set;
import java.util.stream.Collectors;

public class AliOSSLocationsSupplier implements LocationsSupplier {

    private final AliOSSRegionIdsSupplier regionIdsSupplier;

    @Inject
    public AliOSSLocationsSupplier(AliOSSRegionIdsSupplier regionIdsSupplier) {
        this.regionIdsSupplier = regionIdsSupplier;
    }

    @Override
    public Set<? extends Location> get() {
        return regionIdsSupplier.get()
                                .stream()
                                .map(region -> new LocationBuilder().id(region)
                                                                    .scope(LocationScope.REGION)
                                                                    .description(region)
                                                                    .build())
                                .collect(Collectors.toSet());
    }

}

package com.sap.cloud.lm.sl.cf.core.cf.detect.entity;

import org.cloudfoundry.client.lib.domain.CloudService;

import com.sap.cloud.lm.sl.cf.core.model.MtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.ServiceMtaMetadata;

public class ServiceMetadataEntity extends MetadataEntity {
    
    private ServiceMtaMetadata serviceMtaMetadata;
    private CloudService service;

    public ServiceMetadataEntity(ServiceMtaMetadata serviceMtaMetadata, CloudService service, MtaMetadata mtaMetadata) {
        super(mtaMetadata);
        this.serviceMtaMetadata = serviceMtaMetadata;
        this.service = service;
    }

    public ServiceMtaMetadata getServiceMtaMetadata() {
        return serviceMtaMetadata;
    }

    public CloudService getService() {
        return service;
    }
}

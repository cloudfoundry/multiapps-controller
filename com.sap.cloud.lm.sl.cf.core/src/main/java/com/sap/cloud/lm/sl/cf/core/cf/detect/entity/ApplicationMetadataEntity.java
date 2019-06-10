package com.sap.cloud.lm.sl.cf.core.cf.detect.entity;

import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.core.model.ApplicationMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.MtaMetadata;

public class ApplicationMetadataEntity extends MetadataEntity {
    
    private ApplicationMtaMetadata applicationMtaMetadata;
    private CloudApplication application;

    public ApplicationMetadataEntity(MtaMetadata mtaMetadata, ApplicationMtaMetadata applicationMtaMetadata, CloudApplication application) {
        super(mtaMetadata);
        this.applicationMtaMetadata = applicationMtaMetadata;
        this.application = application;
    }

    public ApplicationMtaMetadata getApplicationMtaMetadata() {
        return applicationMtaMetadata;
    }

    public CloudApplication getApplication() {
        return application;
    }
}

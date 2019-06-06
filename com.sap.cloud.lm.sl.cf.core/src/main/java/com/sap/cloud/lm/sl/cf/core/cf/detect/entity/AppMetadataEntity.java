package com.sap.cloud.lm.sl.cf.core.cf.detect.entity;

import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.core.model.ApplicationMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaMetadata;

public class AppMetadataEntity extends MtaMetadataEntity {
    
    private ApplicationMtaMetadata applicationMtaMetadata;
    private CloudApplication app;

    public AppMetadataEntity(ApplicationMtaMetadata applicationMtaMetadata, CloudApplication app, DeployedMtaMetadata deployedMtaMetadata) {
        super(deployedMtaMetadata);
        this.applicationMtaMetadata = applicationMtaMetadata;
        this.app = app;
    }

    public ApplicationMtaMetadata getApplicationMtaMetadata() {
        return applicationMtaMetadata;
    }

    public CloudApplication getApp() {
        return app;
    }
}

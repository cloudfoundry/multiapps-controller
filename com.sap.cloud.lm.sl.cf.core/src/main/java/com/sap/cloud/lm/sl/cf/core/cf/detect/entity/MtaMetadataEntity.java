package com.sap.cloud.lm.sl.cf.core.cf.detect.entity;

import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaMetadata;

public class MtaMetadataEntity {
    
    public MtaMetadataEntity(DeployedMtaMetadata mtaMetadata) {
        this.mtaMetadata = mtaMetadata;
    }

    private DeployedMtaMetadata mtaMetadata;

    public DeployedMtaMetadata getMtaMetadata() {
        return mtaMetadata;
    }

    public void setMtaMetadata(DeployedMtaMetadata mtaMetadata) {
        this.mtaMetadata = mtaMetadata;
    }
}

package com.sap.cloud.lm.sl.cf.core.cf.detect.entity;

import com.sap.cloud.lm.sl.cf.core.model.MtaMetadata;

public class MetadataEntity {
    
    private MtaMetadata mtaMetadata;

    public MetadataEntity(MtaMetadata mtaMetadata) {
        this.mtaMetadata = mtaMetadata;
    }

    public MtaMetadata getMtaMetadata() {
        return mtaMetadata;
    }

    public void setMtaMetadata(MtaMetadata mtaMetadata) {
        this.mtaMetadata = mtaMetadata;
    }
}

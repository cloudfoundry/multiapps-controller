package com.sap.cloud.lm.sl.cf.core.cf.detect;

import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.MetadataEntity;

public interface MtaMetadataExtractorFactory<T extends MetadataEntity> {

    MtaMetadataExtractor<T> get(T e);

}
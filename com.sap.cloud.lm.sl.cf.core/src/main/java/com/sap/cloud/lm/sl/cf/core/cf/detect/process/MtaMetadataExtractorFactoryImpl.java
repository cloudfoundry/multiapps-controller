package com.sap.cloud.lm.sl.cf.core.cf.detect.process;

import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.detect.MtaMetadataExtractor;
import com.sap.cloud.lm.sl.cf.core.cf.detect.MtaMetadataExtractorFactory;
import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.ApplicationMetadataEntity;
import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.MetadataEntity;
import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.ServiceMetadataEntity;

@Component
public class MtaMetadataExtractorFactoryImpl<T extends MetadataEntity> implements MtaMetadataExtractorFactory<T> {

    @Override
    public MtaMetadataExtractor<T> get(T entity) {
        if(entity instanceof ApplicationMetadataEntity) {
            return (MtaMetadataExtractor<T>) new AppMtaMetadataExtractor();
        }
        if(entity instanceof ServiceMetadataEntity) {
            return (MtaMetadataExtractor<T>) new ServiceMtaMetadataExtractor();
        }
        return null;
    }
    
}

package com.sap.cloud.lm.sl.cf.core.cf.detect.process;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.detect.MtaMetadataExtractor;
import com.sap.cloud.lm.sl.cf.core.cf.detect.MtaMetadataExtractorFactory;
import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.AppMetadataEntity;
import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.MtaMetadataEntity;
import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.ServiceMetadataEntity;

@Component
public class MtaMetadataExtractorFactoryImpl<T extends MtaMetadataEntity> implements MtaMetadataExtractorFactory<T> {

    @Override
    public MtaMetadataExtractor<T> get(T entity) {
        if(entity instanceof AppMetadataEntity) {
            return (MtaMetadataExtractor<T>) new AppMtaMetadataExtractor();
        }
        if(entity instanceof ServiceMetadataEntity) {
            return (MtaMetadataExtractor<T>) new ServiceMtaMetadataExtractor();
        }
        return null;
    }
    
}

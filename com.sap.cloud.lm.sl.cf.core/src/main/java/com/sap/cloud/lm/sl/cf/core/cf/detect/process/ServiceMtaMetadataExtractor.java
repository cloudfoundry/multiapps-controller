package com.sap.cloud.lm.sl.cf.core.cf.detect.process;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.detect.MtaMetadataExtractor;
import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.ServiceMetadataEntity;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;

public class ServiceMtaMetadataExtractor implements MtaMetadataExtractor<ServiceMetadataEntity> {

    @Override
    public void extract(ServiceMetadataEntity metadataEntity, DeployedMta metadata) {
        initMetadata(metadataEntity, metadata);
        if (metadataEntity.getServiceMtaMetadata() == null || metadataEntity.getServiceMtaMetadata().getBoundApps() == null) {
            return;
        }

        if (metadata.getMetadata() == null) {
            metadata.setMetadata(metadataEntity.getMtaMetadata());
        }

        
        for (String appName : metadataEntity.getServiceMtaMetadata().getBoundApps()) {
            DeployedMtaModule defaultModule = DeployedMtaModule.builder().withAppName(appName).build();
            
            DeployedMtaModule module = metadata.getModules()
                                               .stream()
                                               .filter(e -> e.getAppName().equalsIgnoreCase(appName))
                                               .findFirst()
                                               .orElse(defaultModule);
            module.getServices().add(metadataEntity.getServiceMtaMetadata().getDeployedMtaResource());
            metadata.getModules().add(module);
            metadata.getServices().add(metadataEntity.getServiceMtaMetadata().getDeployedMtaResource());
        }
    }

}

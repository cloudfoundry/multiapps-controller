package com.sap.cloud.lm.sl.cf.core.cf.detect.process;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.core.cf.detect.MtaMetadataExtractor;
import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.AppMetadataEntity;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaResource;

public class AppMtaMetadataExtractor implements MtaMetadataExtractor<AppMetadataEntity> {

    @Override
    public void extract(AppMetadataEntity metadataEntity, DeployedMta metadata) {
        initMetadata(metadataEntity, metadata);

        CloudApplication app = metadataEntity.getApp();
        ApplicationMtaMetadata appMetadata = metadataEntity.getApplicationMtaMetadata();

        String appName = app.getName();
        DeployedMtaModule module = metadata.getModules()
                                           .stream()
                                           .filter(e -> e.getAppName().equalsIgnoreCase(appName))
                                           .findFirst()
                                           .orElse(addNewModule(metadata));

        String moduleName = (appMetadata.getModuleName() != null) ? appMetadata.getModuleName() : appName;
        List<String> providedDependencies = (appMetadata.getProvidedDependencyNames() != null) ? appMetadata.getProvidedDependencyNames()
            : new ArrayList<>();
        List<DeployedMtaResource> appServices = (appMetadata.getServices() != null) ? appMetadata.getServices() : new ArrayList<>();
        Date createdOn = app.getMeta().getCreated();
        Date updatedOn = app.getMeta().getUpdated();

        module.setModuleName(moduleName);
        module.setAppName(appName);
        module.setCreatedOn(createdOn);
        module.setUpdatedOn(updatedOn);
        module.setProvidedDependencyNames(providedDependencies);
        module.setUris(app.getUris());

        appServices.stream()
                   .filter(appService -> !containsService(appService, module.getServices()))
                   .forEach(s -> module.getServices().add(s));
    }

    private boolean containsService(DeployedMtaResource appService, List<DeployedMtaResource> services) {
        return services.stream()
                       .filter(moduleService -> moduleService.getResourceName().equalsIgnoreCase(appService.getResourceName()))
                       .findAny()
                       .isPresent();
    }

    private DeployedMtaModule addNewModule(DeployedMta metadata) {
        DeployedMtaModule module = DeployedMtaModule.builder().build();
        metadata.getModules().add(module);
        return module;
    }
}

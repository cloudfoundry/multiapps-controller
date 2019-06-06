package com.sap.cloud.lm.sl.cf.core.cf.detect.process;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.core.cf.detect.MtaMetadataExtractor;
import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.AppMetadataEntity;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaResource;

public class AppMtaMetadataExtractor implements MtaMetadataExtractor<AppMetadataEntity> {

    @Override
    public void extract(AppMetadataEntity metadataEntity, DeployedMta deployedMta) {
        initMetadata(metadataEntity, deployedMta);

        CloudApplication app = metadataEntity.getApp();
        ApplicationMtaMetadata appMetadata = metadataEntity.getApplicationMtaMetadata();

        String appName = app.getName();
        DeployedMtaModule module = deployedMta.getModules()
                                              .stream()
                                              .filter(mtaModule -> mtaModule.getAppName()
                                                                            .equalsIgnoreCase(appName))
                                              .findFirst()
                                              .orElse(addNewModule(deployedMta));
        module.setValid(true);

        String moduleName = (appMetadata.getModule()
                                        .getModuleName() != null) ? appMetadata.getModule()
                                                                               .getModuleName()
                                            : appName;
        List<String> providedDependencies = (appMetadata.getModule()
                                                        .getProvidedDependencyNames() != null) ? appMetadata.getModule()
                                                                                                            .getProvidedDependencyNames()
                                                            : new ArrayList<>();
        List<DeployedMtaResource> appServices = (appMetadata.getModule()
                                                            .getServices() != null) ? appMetadata.getModule()
                                                                                                 .getServices()
                                                                : new ArrayList<>();
        Date createdOn = app.getMetadata()
                            .getCreatedAt();
        Date updatedOn = app.getMetadata()
                            .getUpdatedAt();

        module.setModuleName(moduleName);
        module.setAppName(appName);
        module.setCreatedOn(createdOn);
        module.setUpdatedOn(updatedOn);
        module.setProvidedDependencyNames(providedDependencies);
        module.setUris(app.getUris());

        appServices.stream() // Do not replace existing module resources. They might be created by service metadata extraction.
                   .filter(resource -> !containsResource(module.getServices(), resource))
                   .forEach(resource -> module.getServices()
                                              .add(resource));

        module.getServices()
              .stream() // Do not replace existing resources. They might be created by service metadata extraction.
              .filter(resource -> !containsResource(deployedMta.getServices(), resource))
              .forEach(resource -> deployedMta.getServices()
                                              .add(resource));
    }

    private boolean containsResource(List<DeployedMtaResource> resources, DeployedMtaResource resource) {
        boolean containsByResourceName = resources.stream()
                                                  .filter(moduleResource -> moduleResource.getResourceName() != null)
                                                  .filter(moduleResource -> moduleResource.getResourceName()
                                                                                          .equalsIgnoreCase(resource.getResourceName()))
                                                  .findAny()
                                                  .isPresent();
        return containsByResourceName || resources.stream()
                                                  .filter(moduleResource -> moduleResource.getServiceName() != null)
                                                  .filter(moduleResource -> moduleResource.getServiceName()
                                                                                          .equalsIgnoreCase(resource.getServiceName()))
                                                  .findAny()
                                                  .isPresent();
    }

    private DeployedMtaModule addNewModule(DeployedMta metadata) {
        DeployedMtaModule module = DeployedMtaModule.builder()
                                                    .build();
        metadata.getModules()
                .add(module);
        return module;
    }
}

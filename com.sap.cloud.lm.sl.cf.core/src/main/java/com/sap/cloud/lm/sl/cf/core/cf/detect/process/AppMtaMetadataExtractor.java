package com.sap.cloud.lm.sl.cf.core.cf.detect.process;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.core.cf.detect.MtaMetadataExtractor;
import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.ApplicationMetadataEntity;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaResource;

public class AppMtaMetadataExtractor implements MtaMetadataExtractor<ApplicationMetadataEntity> {

    @Override
    public void extract(ApplicationMetadataEntity metadataEntity, DeployedMta deployedMta) {
        initMetadata(metadataEntity, deployedMta);

        CloudApplication app = metadataEntity.getApplication();
        ApplicationMtaMetadata appMetadata = metadataEntity.getApplicationMtaMetadata();

        String appName = app.getName();
        DeployedMtaModule module = deployedMta.getModules()
                                              .stream()
                                              .filter(mtaModule -> mtaModule.getAppName()
                                                                            .equalsIgnoreCase(appName))
                                              .findFirst()
                                              .orElse(addNewModule(deployedMta));

        String moduleName = (appMetadata.getDeployedMtaModule()
                                        .getModuleName() != null) ? appMetadata.getDeployedMtaModule()
                                                                               .getModuleName()
                                            : appName;

        List<String> providedDependencies = (appMetadata.getDeployedMtaModule()
                                                        .getProvidedDependencyNames() != null) ? appMetadata.getDeployedMtaModule()
                                                                                                            .getProvidedDependencyNames()
                                                            : new ArrayList<>();

        List<DeployedMtaResource> appServices = (appMetadata.getDeployedMtaModule()
                                                            .getResources() != null) ? appMetadata.getDeployedMtaModule()
                                                                                                 .getResources()
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

        appServices.stream()
                   .forEach(resource -> module.getResources()
                                              .add(resource));

        /*
         * Do not replace existing resources. They might be created by service metadata extraction. This is here only to move the user
         * provided service metadata to the service metadata because of v3 metadata api limitations regarding user provided services.
         */
        appServices.stream()
              .filter(resource -> !containsResource(deployedMta.getResources(), resource))
              .forEach(resource -> deployedMta.getResources()
                                              .add(resource));
    }

    private boolean containsResource(List<DeployedMtaResource> resources, DeployedMtaResource resource) {
        boolean containsByResourceName = resources.stream()
                                                  .filter(moduleResource -> moduleResource.getResourceName() != null)
                                                  .anyMatch(moduleResource -> moduleResource.getResourceName()
                                                                                            .equalsIgnoreCase(resource.getResourceName()));
        return containsByResourceName || resources.stream()
                                                  .filter(moduleResource -> moduleResource.getServiceName() != null)
                                                  .anyMatch(moduleResource -> moduleResource.getServiceName()
                                                                                            .equalsIgnoreCase(resource.getServiceName()));
    }

    private DeployedMtaModule addNewModule(DeployedMta metadata) {
        DeployedMtaModule module = DeployedMtaModule.builder()
                                                    .build();
        metadata.getModules()
                .add(module);
        return module;
    }
}

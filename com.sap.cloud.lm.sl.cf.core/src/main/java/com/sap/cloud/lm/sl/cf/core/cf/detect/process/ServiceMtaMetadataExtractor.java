package com.sap.cloud.lm.sl.cf.core.cf.detect.process;

import java.util.List;
import java.util.Optional;

import com.sap.cloud.lm.sl.cf.core.cf.detect.MtaMetadataExtractor;
import com.sap.cloud.lm.sl.cf.core.cf.detect.entity.ServiceMetadataEntity;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMta;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaResource;

public class ServiceMtaMetadataExtractor implements MtaMetadataExtractor<ServiceMetadataEntity> {

    @Override
    public void extract(ServiceMetadataEntity metadataEntity, DeployedMta metadata) {
        initMetadata(metadataEntity, metadata);
        if (metadataEntity.getServiceMtaMetadata() == null || metadataEntity.getServiceMtaMetadata()
                                                                            .getDeployedMtaResource()
                                                                            .getModules() == null) {
            return;
        }

        metadataEntity.getServiceMtaMetadata()
                      .getDeployedMtaResource()
                      .getModules()
                      .forEach(serviceModule -> {
                          DeployedMtaModule module = getMatchingModule(metadata, serviceModule);
                          DeployedMtaResource newMtaResource = metadataEntity.getServiceMtaMetadata()
                                                                             .getDeployedMtaResource();
                          addResource(module.getServices(), newMtaResource);
                          metadata.getServices()
                                  .add(newMtaResource);
                          setModuleInMetadata(metadata, module);
                      });
    }

    private void setModuleInMetadata(DeployedMta metadata, DeployedMtaModule module) {
        boolean moduleDoesNotExistInMetadata = metadata.getModules()
                                                       .stream()
                                                       .noneMatch(mtaModule -> mtaModule.getModuleName()
                                                                                        .equalsIgnoreCase(module.getModuleName()));
        if (moduleDoesNotExistInMetadata) {
            metadata.getModules()
                    .add(module);
        }
    }

    private void addResource(List<DeployedMtaResource> resources, DeployedMtaResource newMtaResource) {
        DeployedMtaResource existingResource = getExistingResource(resources, newMtaResource);
        if (existingResource != null) {
            DeployedMtaResource resource = existingResource;
            resource.setCreatedOn(newMtaResource.getCreatedOn());
            resource.setUpdatedOn(newMtaResource.getUpdatedOn());
            resource.setResourceName(newMtaResource.getResourceName());
            resource.setServiceName(newMtaResource.getServiceName());
            resource.setAppsCredentials(newMtaResource.getAppsCredentials());
            resource.setModules(newMtaResource.getModules());
        } else {
            resources.add(newMtaResource);
        }
    }

    private DeployedMtaResource getExistingResource(List<DeployedMtaResource> resources, DeployedMtaResource resource) {
        Optional<DeployedMtaResource> containedByResourceName = resources.stream()
                                                                         .filter(moduleResource -> moduleResource.getResourceName() != null)
                                                                         .filter(moduleResource -> moduleResource.getResourceName()
                                                                                                                 .equalsIgnoreCase(resource.getResourceName()))
                                                                         .findFirst();
        if (containedByResourceName.isPresent()) {
            return containedByResourceName.get();
        }
        Optional<DeployedMtaResource> containedByServiceName = resources.stream()
                                                                        .filter(moduleResource -> moduleResource.getServiceName() != null)
                                                                        .filter(moduleResource -> moduleResource.getServiceName()
                                                                                                                .equalsIgnoreCase(resource.getServiceName()))
                                                                        .findFirst();
        if (containedByServiceName.isPresent()) {
            return containedByServiceName.get();
        }
        return null;
    }

    private DeployedMtaModule getMatchingModule(DeployedMta metadata, DeployedMtaModule serviceModule) {
        DeployedMtaModule defaultModule = DeployedMtaModule.builder()
                                                           .withModuleName(serviceModule.getModuleName())
                                                           .withAppName(serviceModule.getAppName())
                                                           .withIsValid(false)
                                                           .build();
        return metadata.getModules()
                       .stream()
                       .filter(moduleMetadata -> moduleMetadata.getModuleName()
                                                               .equalsIgnoreCase(serviceModule.getModuleName()))
                       .findFirst()
                       .orElse(defaultModule);
    }

}

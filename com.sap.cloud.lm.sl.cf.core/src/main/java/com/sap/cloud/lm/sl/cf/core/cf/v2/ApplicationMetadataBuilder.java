package com.sap.cloud.lm.sl.cf.core.cf.v2;

import com.sap.cloud.lm.sl.cf.core.cf.detect.mapping.ApplicationMetadataFieldExtractor;
import com.sap.cloud.lm.sl.cf.core.cf.detect.mapping.MetadataFieldExtractor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaResource;
import com.sap.cloud.lm.sl.cf.core.model.SupportedParameters;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;
import com.sap.cloud.lm.sl.mta.model.ProvidedDependency;
import com.sap.cloud.lm.sl.mta.model.Resource;
import org.cloudfoundry.client.v3.Metadata;

import java.util.*;
import java.util.stream.Collectors;

public class ApplicationMetadataBuilder {
    public static Metadata build(DeploymentDescriptor deploymentDescriptor, Module module, List<ResourceAndResourceType> moduleResources,
        List<String> uris) {
        List<DeployedMtaResource> deployedResources = moduleResources.stream()
                                                                     .map(resource -> mapResourceToDeployedMtaResource(resource, module))
                                                                     .collect(Collectors.toList());

        List<String> providedDependenciesNames = module.getProvidedDependencies()
                                                       .stream()
                                                       .filter(ProvidedDependency::isPublic)
                                                       .map(ProvidedDependency::getName)
                                                       .collect(Collectors.toList());

        DeployedMtaModule deployedMtaModule = DeployedMtaModule.builder()
                                                               .withModuleName(module.getName())
                                                               .withAppName(NameUtil.getApplicationName(module))
                                                               .withProvidedDependencyNames(providedDependenciesNames)
                                                               .withServices(deployedResources)
                                                               .withUris(uris)
                                                               .build();

        return Metadata.builder()
                       .label(MetadataFieldExtractor.MTA_ID, deploymentDescriptor.getId())
                       .label(MetadataFieldExtractor.MTA_VERSION, deploymentDescriptor.getVersion())
                       .annotation(ApplicationMetadataFieldExtractor.MODULE, JsonUtil.toJson(deployedMtaModule, true))
                       .build();
    }

    private static DeployedMtaResource mapResourceToDeployedMtaResource(ResourceAndResourceType applicationService, Module module) {
        ResourceType resourceType = applicationService.getResourceType();
        Resource resource = applicationService.getResource();
        if (resourceType != ResourceType.USER_PROVIDED_SERVICE) {
            return DeployedMtaResource.builder()
                                      .withServiceName(NameUtil.getServiceName(resource))
                                      .withResourceName(resource.getName())
                                      .build();
        }

        List<DeployedMtaModule> deployedMtaModules = Collections.singletonList(DeployedMtaModule.builder()
                                                                                                .withModuleName(module.getName())
                                                                                                .withAppName(NameUtil.getApplicationName(module))
                                                                                                .build());
        Map<String, Object> parameters = (Map<String, Object>) resource.getParameters()
                                                                       .getOrDefault(SupportedParameters.SERVICE_CONFIG,
                                                                                     Collections.emptyMap());
        TreeMap<String, Object> credentials = new TreeMap<>(parameters);
        return DeployedMtaResource.builder()
                                  .withServiceName(resource.getName())
                                  .withServiceName(NameUtil.getServiceName(resource))
                                  .withModules(deployedMtaModules)
                                  .withAppsCredentials(credentials)
                                  .build();
    }
}

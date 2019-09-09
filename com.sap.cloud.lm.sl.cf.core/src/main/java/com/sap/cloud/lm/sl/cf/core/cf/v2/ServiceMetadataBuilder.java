package com.sap.cloud.lm.sl.cf.core.cf.v2;

import com.sap.cloud.lm.sl.cf.core.cf.detect.mapping.MetadataFieldExtractor;
import com.sap.cloud.lm.sl.cf.core.cf.detect.mapping.ServiceMetadataFieldExtractor;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaResource;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;
import com.sap.cloud.lm.sl.mta.model.Resource;
import org.cloudfoundry.client.v3.Metadata;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ServiceMetadataBuilder {

    public static Metadata build(DeploymentDescriptor deploymentDescriptor, Resource resource, Map<String, Object> serviceParameters) {

        List<DeployedMtaModule> boundModules = deploymentDescriptor.getModules()
                                                                   .stream()
                                                                   .filter(module -> moduleContainsResource(module, resource))
                                                                   .map(ServiceMetadataBuilder::mapModuleToDeployedMtaModule)
                                                                   .collect(Collectors.toList());

        DeployedMtaResource deployedMtaResource = DeployedMtaResource.builder()
                                                                     .withServiceName(NameUtil.getServiceName(resource))
                                                                     .withResourceName(resource.getName())
                                                                     .withServiceInstanceParameters(serviceParameters)
                                                                     .withModules(boundModules)
                                                                     .build();

        return Metadata.builder()
                       .label(MetadataFieldExtractor.MTA_ID, deploymentDescriptor.getId())
                       .label(MetadataFieldExtractor.MTA_VERSION, deploymentDescriptor.getVersion())
                       .annotation(ServiceMetadataFieldExtractor.RESOURCE, JsonUtil.toJson(deployedMtaResource, true))
                       .build();
    }

    private static boolean moduleContainsResource(Module module, Resource resource) {
        return module.getRequiredDependencies()
                     .stream()
                     .anyMatch(dependency -> dependency.getName()
                                                       .equalsIgnoreCase(
                                                           resource.getName()));
    }

    private static DeployedMtaModule mapModuleToDeployedMtaModule(Module module) {
        return DeployedMtaModule.builder()
                                .withAppName(NameUtil.getApplicationName(module))
                                .withModuleName(module.getName())
                                .build();
    }
}

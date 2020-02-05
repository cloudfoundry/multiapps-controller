package com.sap.cloud.lm.sl.cf.core.cf.v2;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.cloudfoundry.client.v3.Metadata;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadataAnnotations;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadataLabels;
import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Module;
import com.sap.cloud.lm.sl.mta.model.ProvidedDependency;

public class ApplicationMetadataBuilder {

    public static Metadata build(DeploymentDescriptor deploymentDescriptor, Module module, List<ResourceAndResourceType> resources) {
        String mtaModuleAnnotation = buildMtaModuleAnnotation(module);
        String mtaModuleProvidedDependenciesAnnotation = buildMtaModuleProvidedDependenciesAnnotation(module);
        String mtaServicesAnnotation = buildBoundMtaServicesAnnotation(resources);
        return Metadata.builder()
                       .label(MtaMetadataLabels.MTA_ID, deploymentDescriptor.getId())
                       .label(MtaMetadataLabels.MTA_VERSION, deploymentDescriptor.getVersion())
                       .annotation(MtaMetadataAnnotations.MTA_MODULE, mtaModuleAnnotation)
                       .annotation(MtaMetadataAnnotations.MTA_MODULE_PUBLIC_PROVIDED_DEPENDENCIES, mtaModuleProvidedDependenciesAnnotation)
                       .annotation(MtaMetadataAnnotations.MTA_MODULE_BOUND_SERVICES, mtaServicesAnnotation)
                       .build();
    }

    private static String buildMtaModuleAnnotation(Module module) {
        Map<String, String> mtaModule = new TreeMap<>();
        MapUtil.addNonNull(mtaModule, Constants.ATTR_NAME, module.getName());
        return JsonUtil.toJson(mtaModule);
    }

    private static String buildMtaModuleProvidedDependenciesAnnotation(Module module) {
        List<String> providedDependencies = module.getProvidedDependencies()
                                                  .stream()
                                                  .filter(ProvidedDependency::isPublic)
                                                  .map(ProvidedDependency::getName)
                                                  .collect(Collectors.toList());
        return JsonUtil.toJson(providedDependencies);
    }

    private static String buildBoundMtaServicesAnnotation(List<ResourceAndResourceType> resources) {
        List<String> mtaServices = resources.stream()
                                            .map(ResourceAndResourceType::getResource)
                                            .map(NameUtil::getServiceName)
                                            .collect(Collectors.toList());
        return JsonUtil.toJson(mtaServices);
    }

}

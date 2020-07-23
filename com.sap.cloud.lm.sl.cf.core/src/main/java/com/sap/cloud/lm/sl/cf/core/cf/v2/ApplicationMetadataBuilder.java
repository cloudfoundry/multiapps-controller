package com.sap.cloud.lm.sl.cf.core.cf.v2;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.ProvidedDependency;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadataAnnotations;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadataLabels;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.util.MtaMetadataUtil;

public class ApplicationMetadataBuilder {

    public static Metadata build(DeploymentDescriptor deploymentDescriptor, String namespace, Module module, List<String> services) {
        String hashedMtaId = MtaMetadataUtil.getHashedLabel(deploymentDescriptor.getId());
        String mtaModuleAnnotation = buildMtaModuleAnnotation(module);
        String mtaModuleProvidedDependenciesAnnotation = buildMtaModuleProvidedDependenciesAnnotation(module);
        String mtaServicesAnnotation = buildBoundMtaServicesAnnotation(services);

        Metadata.Builder builder = Metadata.builder()
                                           .label(MtaMetadataLabels.MTA_ID, hashedMtaId)
                                           .annotation(MtaMetadataAnnotations.MTA_ID, deploymentDescriptor.getId())
                                           .annotation(MtaMetadataAnnotations.MTA_VERSION, deploymentDescriptor.getVersion())
                                           .annotation(MtaMetadataAnnotations.MTA_MODULE, mtaModuleAnnotation)
                                           .annotation(MtaMetadataAnnotations.MTA_MODULE_PUBLIC_PROVIDED_DEPENDENCIES,
                                                       mtaModuleProvidedDependenciesAnnotation)
                                           .annotation(MtaMetadataAnnotations.MTA_MODULE_BOUND_SERVICES, mtaServicesAnnotation);

        if (StringUtils.isNotEmpty(namespace)) {
            String hashedMtaNamespace = MtaMetadataUtil.getHashedLabel(namespace);
            builder.label(MtaMetadataLabels.MTA_NAMESPACE, hashedMtaNamespace)
                   .annotation(MtaMetadataAnnotations.MTA_NAMESPACE, namespace);
        }

        return builder.build();
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

    private static String buildBoundMtaServicesAnnotation(List<String> services) {
        return JsonUtil.toJson(services);
    }

    private ApplicationMetadataBuilder() {
    }

}

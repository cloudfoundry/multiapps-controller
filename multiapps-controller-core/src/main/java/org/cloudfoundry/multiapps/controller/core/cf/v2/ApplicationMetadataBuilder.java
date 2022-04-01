package org.cloudfoundry.multiapps.controller.core.cf.v2;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataAnnotations;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Module;
import org.cloudfoundry.multiapps.mta.model.ProvidedDependency;

public class ApplicationMetadataBuilder {

    public static Metadata build(DeploymentDescriptor deploymentDescriptor, String namespace, Module module, List<String> services) {
        String mtaModuleAnnotation = buildMtaModuleAnnotation(module);
        String mtaModuleProvidedDependenciesAnnotation = buildMtaModuleProvidedDependenciesAnnotation(module);
        String mtaServicesAnnotation = buildBoundMtaServicesAnnotation(services);

        Metadata.Builder builder = MtaMetadataBuilder.init(deploymentDescriptor, namespace)
                                           .annotation(MtaMetadataAnnotations.MTA_MODULE, mtaModuleAnnotation)
                                           .annotation(MtaMetadataAnnotations.MTA_MODULE_PUBLIC_PROVIDED_DEPENDENCIES,
                                                       mtaModuleProvidedDependenciesAnnotation)
                                           .annotation(MtaMetadataAnnotations.MTA_MODULE_BOUND_SERVICES, mtaServicesAnnotation);

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

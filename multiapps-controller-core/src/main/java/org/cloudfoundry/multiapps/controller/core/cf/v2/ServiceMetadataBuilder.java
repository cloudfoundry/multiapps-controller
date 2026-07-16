package org.cloudfoundry.multiapps.controller.core.cf.v2;

import java.util.Map;
import java.util.TreeMap;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.client.v3.Metadata.Builder;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.MapUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CfUserMetadata;
import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataAnnotations;
import org.cloudfoundry.multiapps.mta.model.DeploymentDescriptor;
import org.cloudfoundry.multiapps.mta.model.Resource;

public class ServiceMetadataBuilder {

    public static Metadata build(DeploymentDescriptor deploymentDescriptor, String namespace, Resource resource,
                                 CfUserMetadata userCfMetadata) {
        String mtaResourceAnnotation = buildMtaResourceAnnotation(resource);

        Builder builder = MtaMetadataBuilder.init(deploymentDescriptor, namespace)
                                            .annotation(MtaMetadataAnnotations.MTA_RESOURCE, mtaResourceAnnotation);

        if (userCfMetadata != null) {
            userCfMetadata.getLabels()
                          .forEach(builder::label);
            userCfMetadata.getAnnotations()
                          .forEach(builder::annotation);
        }

        return builder.build();
    }

    private static String buildMtaResourceAnnotation(Resource resource) {
        Map<String, String> mtaModule = new TreeMap<>();
        MapUtil.addNonNull(mtaModule, Constants.ATTR_NAME, resource.getName());
        return JsonUtil.toJson(mtaModule);
    }

    private ServiceMetadataBuilder() {
    }

}

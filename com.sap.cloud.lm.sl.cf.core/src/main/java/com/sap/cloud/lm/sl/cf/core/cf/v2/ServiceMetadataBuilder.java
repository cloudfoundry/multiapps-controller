package com.sap.cloud.lm.sl.cf.core.cf.v2;

import java.util.Map;
import java.util.TreeMap;

import org.cloudfoundry.client.v3.Metadata;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadataAnnotations;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadataLabels;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.util.MtaMetadataUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.MapUtil;
import com.sap.cloud.lm.sl.mta.model.DeploymentDescriptor;
import com.sap.cloud.lm.sl.mta.model.Resource;

public class ServiceMetadataBuilder {

    public static Metadata build(DeploymentDescriptor deploymentDescriptor, Resource resource) {
        String mtaResourceAnnotation = buildMtaResourceAnnotation(resource);
        String hashedMtaId = MtaMetadataUtil.getHashedMtaId(deploymentDescriptor.getId());
        return Metadata.builder()
                       .label(MtaMetadataLabels.MTA_ID, hashedMtaId)
                       .label(MtaMetadataLabels.MTA_VERSION, null)
                       .annotation(MtaMetadataAnnotations.MTA_ID, deploymentDescriptor.getId())
                       .annotation(MtaMetadataAnnotations.MTA_VERSION, deploymentDescriptor.getVersion())
                       .annotation(MtaMetadataAnnotations.MTA_RESOURCE, mtaResourceAnnotation)
                       .build();
    }

    private static String buildMtaResourceAnnotation(Resource resource) {
        Map<String, String> mtaModule = new TreeMap<>();
        MapUtil.addNonNull(mtaModule, Constants.ATTR_NAME, resource.getName());
        return JsonUtil.toJson(mtaModule);
    }

    private ServiceMetadataBuilder() {
    }

}

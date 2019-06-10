package com.sap.cloud.lm.sl.cf.core.cf.detect.mapping;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.v3.Metadata;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.cf.core.model.MtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaResource;
import com.sap.cloud.lm.sl.cf.core.model.ServiceMtaMetadata;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Component
public class ServiceMetadataFieldExtractor extends MetadataFieldExtractor {

    public static final String RESOURCE = "resource";

    private DeployedMtaResource getResource(Metadata metadata) {
        String resourceJson = metadata.getAnnotations()
                                      .get(RESOURCE);
        return resourceJson == null ? null : JsonUtil.fromJson(resourceJson, new TypeReference<DeployedMtaResource>() {
        });
    }

    public ServiceMtaMetadata extractMetadata(CloudService service) {
        if (service.getV3Metadata() == null) {
            return null;
        }

        MtaMetadata mtaMetadata = new MtaMetadata();
        mtaMetadata.setId(getMtaId(service.getV3Metadata()));
        mtaMetadata.setVersion(getMtaVersion(service.getV3Metadata()));

        DeployedMtaResource resource = getResource(service.getV3Metadata());

        return ServiceMtaMetadata.builder()
                                 .withDeployedMtaResource(resource)
                                 .withMtaMetadata(mtaMetadata)
                                 .build();
    }

}

package com.sap.cloud.lm.sl.cf.core.cf.detect.mapping;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.ParsingException;
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
        if (metadata.getAnnotations() == null) {
            throw new ParsingException(Messages.CANT_PARSE_MTA_METADATA_ANNOTATIONS);
        }
        String resourceJson = metadata.getAnnotations()
                                      .get(RESOURCE);
        if (resourceJson == null) {
            throw new ParsingException(Messages.CANT_PARSE_MTA_METADATA_ANNOTATIONS);
        }
        return JsonUtil.fromJson(resourceJson, new TypeReference<DeployedMtaResource>() {
        });
    }

    public ServiceMtaMetadata extractMetadata(CloudService service) {
        if (service.getV3Metadata() == null) {
            return null;
        }

        try {
            MtaMetadata mtaMetadata = new MtaMetadata();
            mtaMetadata.setId(getMtaId(service.getV3Metadata()));
            mtaMetadata.setVersion(getMtaVersion(service.getV3Metadata()));

            DeployedMtaResource resource = getResource(service.getV3Metadata());

            return ServiceMtaMetadata.builder()
                                     .withDeployedMtaResource(resource)
                                     .withMtaMetadata(mtaMetadata)
                                     .build();
        } catch (ParsingException e) {
            throw new ParsingException(e, Messages.CANT_PARSE_MTA_METADATA_FOR_SERVICE_0, service.getName());
        }
    }
}

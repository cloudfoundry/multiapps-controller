package com.sap.cloud.lm.sl.cf.core.cf.detect.mapping;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.ParsingException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.v3.Metadata;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.MtaMetadata;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

@Component
public class ApplicationMetadataFieldExtractor extends MetadataFieldExtractor {

    public static final String MODULE = "module";

    private DeployedMtaModule getModule(Metadata metadata) {
        if (metadata.getAnnotations() == null) {
            throw new ParsingException(Messages.CANT_PARSE_MTA_METADATA_ANNOTATIONS);
        }
        final String moduleJson = metadata.getAnnotations()
                                          .get(MODULE);
        if (moduleJson == null) {
            throw new ParsingException(Messages.CANT_PARSE_MTA_METADATA_ANNOTATIONS);
        }
        return JsonUtil.fromJson(moduleJson,
                                 new TypeReference<DeployedMtaModule>() {
                                 });
    }

    public ApplicationMtaMetadata extractMetadata(CloudApplication app) {
        if (app.getV3Metadata() == null) {
            return null;
        }
        try {
            MtaMetadata mtaMetadata = new MtaMetadata();
            mtaMetadata.setId(getMtaId(app.getV3Metadata()));
            mtaMetadata.setVersion(getMtaVersion(app.getV3Metadata()));
            DeployedMtaModule module = getModule(app.getV3Metadata());
            return ApplicationMtaMetadata.builder()
                                         .withModule(module)
                                         .withMtaMetadata(mtaMetadata)
                                         .build();
        } catch (ParsingException e) {
            throw new ParsingException(e, Messages.CANT_PARSE_MTA_METADATA_FOR_APP_0, app.getName());
        }
    }
}

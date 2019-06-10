package com.sap.cloud.lm.sl.cf.core.cf.detect.mapping;

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

    private DeployedMtaModule getModule(Metadata v3Metadata) {
        final String moduleJson = v3Metadata.getAnnotations().get(MODULE);
        return moduleJson == null ? null : JsonUtil.fromJson(moduleJson,
                                 new TypeReference<DeployedMtaModule>() {
                                 });
    }

    public ApplicationMtaMetadata extractMetadata(CloudApplication app) {
        if (app.getMetadata() == null) {
            return null;
        }

        MtaMetadata mtaMetadata = new MtaMetadata();
        mtaMetadata.setId(getMtaId(app.getV3Metadata()));
        mtaMetadata.setVersion(getMtaVersion(app.getV3Metadata()));
        DeployedMtaModule module = getModule(app.getV3Metadata());
        return ApplicationMtaMetadata.builder()
                                     .withModule(module)
                                     .withMtaMetadata(mtaMetadata)
                                     .build();
    }
}

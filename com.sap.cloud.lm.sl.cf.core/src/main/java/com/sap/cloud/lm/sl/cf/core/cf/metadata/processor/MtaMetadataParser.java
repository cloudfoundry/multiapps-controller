package com.sap.cloud.lm.sl.cf.core.cf.metadata.processor;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudEntity;
import org.cloudfoundry.client.lib.domain.CloudService;

import com.sap.cloud.lm.sl.cf.core.Messages;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.ImmutableMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadata;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadataAnnotations;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadataLabels;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaService;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMtaService;

@Named
public class MtaMetadataParser extends BaseMtaMetadataParser {

    private MtaMetadataValidator mtaMetadataValidator;

    @Inject
    public MtaMetadataParser(MtaMetadataValidator mtaMetadataValidator) {
        this.mtaMetadataValidator = mtaMetadataValidator;
    }

    public MtaMetadata parseMtaMetadata(CloudEntity entity) {
        mtaMetadataValidator.validate(entity);
        String mtaId = getMtaId(entity);
        String mtaVersion = getMtaVersion(entity);
        String messageOnParsingException = MessageFormat.format(Messages.CANT_PARSE_MTA_METADATA_VERSION_FOR_0, entity.getName());
        return ImmutableMtaMetadata.builder()
                                   .id(mtaId)
                                   .version(parseMtaVersion(mtaVersion, messageOnParsingException))
                                   .build();
    }

    private String getMtaId(CloudEntity entity) {
        String mtaIdFromAnnotations = entity.getV3Metadata()
                                            .getAnnotations()
                                            .get(MtaMetadataAnnotations.MTA_ID);
        if (mtaIdFromAnnotations == null) {
            return entity.getV3Metadata()
                         .getLabels()
                         .get(MtaMetadataLabels.MTA_ID);
        }
        return mtaIdFromAnnotations;
    }

    private String getMtaVersion(CloudEntity entity) {
        String mtaVersionFromAnnotations = entity.getV3Metadata()
                                                 .getAnnotations()
                                                 .get(MtaMetadataAnnotations.MTA_VERSION);
        if (mtaVersionFromAnnotations == null) {
            return entity.getV3Metadata()
                         .getLabels()
                         .get(MtaMetadataLabels.MTA_VERSION);
        }
        return mtaVersionFromAnnotations;
    }

    public DeployedMtaApplication parseDeployedMtaApplication(CloudApplication application) {
        mtaMetadataValidator.validate(application);
        Map<String, String> metadataAnnotations = application.getV3Metadata()
                                                             .getAnnotations();
        String moduleName = parseNameAttribute(metadataAnnotations, MtaMetadataAnnotations.MTA_MODULE);
        List<String> providedDependencies = parseModuleProvidedDependencies(application.getName(), metadataAnnotations,
                                                                            MtaMetadataAnnotations.MTA_MODULE_PUBLIC_PROVIDED_DEPENDENCIES);
        List<String> boundMtaServices = parseList(metadataAnnotations, MtaMetadataAnnotations.MTA_MODULE_BOUND_SERVICES);
        return ImmutableDeployedMtaApplication.builder()
                                              .from(application)
                                              .moduleName(moduleName)
                                              .boundMtaServices(boundMtaServices)
                                              .providedDependencyNames(providedDependencies)
                                              .build();
    }

    public DeployedMtaService parseDeployedMtaService(CloudService service) {
        mtaMetadataValidator.validate(service);
        String resourceName = parseNameAttribute(service.getV3Metadata()
                                                        .getAnnotations(),
                                                 MtaMetadataAnnotations.MTA_RESOURCE);
        return ImmutableDeployedMtaService.builder()
                                          .from(service)
                                          .resourceName(resourceName)
                                          .build();
    }

}

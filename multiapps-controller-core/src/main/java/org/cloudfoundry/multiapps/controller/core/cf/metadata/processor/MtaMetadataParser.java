package org.cloudfoundry.multiapps.controller.core.cf.metadata.processor;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.ImmutableMtaMetadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadataAnnotations;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaService;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaService;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudEntity;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;

@Named
public class MtaMetadataParser extends BaseMtaMetadataParser {

    private MtaMetadataValidator mtaMetadataValidator;

    @Inject
    public MtaMetadataParser(MtaMetadataValidator mtaMetadataValidator) {
        this.mtaMetadataValidator = mtaMetadataValidator;
    }

    public String parseQualifiedMtaId(CloudEntity entity) {
        mtaMetadataValidator.validateHasCommonMetadata(entity);
        Metadata metadata = entity.getV3Metadata();
        String mtaId = metadata.getAnnotations()
                               .get(MtaMetadataAnnotations.MTA_ID);
        String mtaNamespace = metadata.getAnnotations()
                                      .get(MtaMetadataAnnotations.MTA_NAMESPACE);

        if (StringUtils.isEmpty(mtaNamespace)) {
            return mtaId;
        }

        return mtaNamespace + Constants.NAMESPACE_SEPARATOR + mtaId;
    }

    public MtaMetadata parseMtaMetadata(CloudEntity entity) {
        mtaMetadataValidator.validateHasCommonMetadata(entity);
        Metadata metadata = entity.getV3Metadata();
        String mtaId = metadata.getAnnotations()
                               .get(MtaMetadataAnnotations.MTA_ID);
        String mtaVersion = metadata.getAnnotations()
                                    .get(MtaMetadataAnnotations.MTA_VERSION);
        String mtaNamespace = metadata.getAnnotations()
                                      .get(MtaMetadataAnnotations.MTA_NAMESPACE);
        String messageOnParsingException = MessageFormat.format(Messages.CANT_PARSE_MTA_METADATA_VERSION_FOR_0, entity.getName());
        return ImmutableMtaMetadata.builder()
                                   .id(mtaId)
                                   .version(parseMtaVersion(mtaVersion, messageOnParsingException))
                                   .namespace(mtaNamespace)
                                   .build();
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

    public DeployedMtaService parseDeployedMtaService(CloudServiceInstance serviceInstance) {
        mtaMetadataValidator.validate(serviceInstance);
        String resourceName = parseNameAttribute(serviceInstance.getV3Metadata()
                                                                .getAnnotations(),
                                                 MtaMetadataAnnotations.MTA_RESOURCE);

        return ImmutableDeployedMtaService.builder()
                                          .from(serviceInstance)
                                          .resourceName(resourceName)
                                          .build();
    }

}

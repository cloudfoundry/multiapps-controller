package org.cloudfoundry.multiapps.controller.core.cf.metadata.processor;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.Constants;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.ImmutableMtaMetadata;
import org.cloudfoundry.multiapps.controller.core.cf.metadata.MtaMetadata;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaApplication;
import org.cloudfoundry.multiapps.controller.core.model.ImmutableDeployedMtaApplication;
import org.cloudfoundry.multiapps.mta.model.Version;

@Named
public class EnvMtaMetadataParser extends BaseMtaMetadataParser {

    private EnvMtaMetadataValidator envMtaMetadataValidator;

    @Inject
    public EnvMtaMetadataParser(EnvMtaMetadataValidator envMtaMetadataValidator) {
        this.envMtaMetadataValidator = envMtaMetadataValidator;
    }

    public MtaMetadata parseMtaMetadata(CloudApplication application) {
        envMtaMetadataValidator.validate(application);
        Map<String, Object> mtaMetadata = JsonUtil.convertJsonToMap(application.getEnv()
                                                                               .get(Constants.ENV_MTA_METADATA));
        String mtaId = (String) mtaMetadata.get(Constants.ATTR_ID);
        String version = (String) mtaMetadata.get(Constants.ATTR_VERSION);
        String namespace = (String) mtaMetadata.get(Constants.ATTR_NAMESPACE);
        String messageOnParsingException = MessageFormat.format(Messages.CANT_PARSE_MTA_ENV_METADATA_VERSION_FOR_APP_0,
                                                                application.getName());
        Version mtaVersion = parseMtaVersion(version, messageOnParsingException);
        return ImmutableMtaMetadata.builder()
                                   .id(mtaId)
                                   .version(mtaVersion)
                                   .namespace(namespace)
                                   .build();
    }

    public DeployedMtaApplication parseDeployedMtaApplication(CloudApplication application) {
        envMtaMetadataValidator.validate(application);
        Map<String, String> env = application.getEnv();
        String moduleName = parseNameAttribute(env, Constants.ENV_MTA_MODULE_METADATA);
        List<String> providedDependencyNames = parseModuleProvidedDependencies(application.getName(), env,
                                                                               Constants.ENV_MTA_MODULE_PUBLIC_PROVIDED_DEPENDENCIES);
        List<String> services = parseList(env, Constants.ENV_MTA_SERVICES);
        return ImmutableDeployedMtaApplication.builder()
                                              .from(application)
                                              .moduleName(moduleName)
                                              .boundMtaServices(services)
                                              .providedDependencyNames(providedDependencyNames)
                                              .build();
    }

}

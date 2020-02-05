package com.sap.cloud.lm.sl.cf.core.cf.metadata.processor;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.ImmutableMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.cf.metadata.MtaMetadata;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaApplication;
import com.sap.cloud.lm.sl.cf.core.model.ImmutableDeployedMtaApplication;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.Version;

@Named
public class EnvMtaMetadataParser extends BaseMtaMetadataParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvMtaMetadataParser.class);

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
        String messageOnParsingException = MessageFormat.format(Messages.CANT_PARSE_MTA_ENV_METADATA_VERSION_FOR_APP_0,
                                                                application.getName());
        Version mtaVersion = parseMtaVersion(version, messageOnParsingException);
        return ImmutableMtaMetadata.builder()
                                   .id(mtaId)
                                   .version(mtaVersion)
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

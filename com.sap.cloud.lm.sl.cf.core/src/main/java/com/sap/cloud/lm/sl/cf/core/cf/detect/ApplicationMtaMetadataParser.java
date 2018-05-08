package com.sap.cloud.lm.sl.cf.core.cf.detect;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaMetadata;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.Version;

public class ApplicationMtaMetadataParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationMtaMetadataParser.class);

    public static ApplicationMtaMetadata parseAppMetadata(CloudApplication app) {
        try {
            return attemptToParseAppMetadata(app);
        } catch (ParsingException e) {
            throw new ParsingException(e, Messages.COULD_NOT_PARSE_MTA_METADATA_FOR_APP_0, app.getName());
        }
    }
    
    private static ApplicationMtaMetadata attemptToParseAppMetadata(CloudApplication app) {
        Map<String, String> appEnv = app.getEnvAsMap();
        DeployedMtaMetadata mtaMetadata = parseMtaMetadata(appEnv);
        List<String> services = parseServices(appEnv);
        String moduleName = parseModuleName(appEnv);
        List<String> providedDependencyNames = parseProvidedDependencyNames(app.getName(), appEnv);
        Map<String, Object> deployAttributes = parseDeployAttributes(appEnv);
        
        if (mtaMetadata == null && services == null && moduleName == null && providedDependencyNames == null && deployAttributes == null) {
            return null;
        }
        return new ApplicationMtaMetadata(mtaMetadata, services, moduleName, providedDependencyNames, deployAttributes);
    }

    private static DeployedMtaMetadata parseMtaMetadata(Map<String, String> appEnv) {
        String envValue = appEnv.get(Constants.ENV_MTA_METADATA);
        if (envValue == null) {
            return null;
        }
        Map<String, Object> mtaPropsMap = JsonUtil.convertJsonToMap(envValue);
        String id = (String) mtaPropsMap.get(Constants.ATTR_ID);
        String versionString = (String) mtaPropsMap.get(Constants.ATTR_VERSION);
        Version version = Version.parseVersion(versionString);
        return new DeployedMtaMetadata(id, version);
    }

    private static List<String> parseServices(Map<String, String> appEnv) {
        String envValue = appEnv.get(Constants.ENV_MTA_SERVICES);
        if (envValue == null) {
            return null;
        }
        return JsonUtil.convertJsonToList(envValue, new TypeToken<List<String>>() {
        }.getType());
    }

    private static String parseModuleName(Map<String, String> appEnv) {
        String envValue = appEnv.get(Constants.ENV_MTA_MODULE_METADATA);
        if (envValue == null) {
            return null;
        }
        Map<String, Object> mtaPropsMap = JsonUtil.convertJsonToMap(envValue);
        return (String) mtaPropsMap.get(Constants.ATTR_NAME);
    }

    private static List<String> parseProvidedDependencyNames(String appName, Map<String, String> appEnv) {
        String envValue = appEnv.get(Constants.ENV_MTA_MODULE_PUBLIC_PROVIDED_DEPENDENCIES);
        if (envValue == null) {
            return null;
        }
        try {
            return JsonUtil.convertJsonToList(envValue, new TypeToken<List<String>>() {
            }.getType());
        } catch (ParsingException e) {
            LOGGER.warn(MessageFormat.format(Messages.COULD_NOT_PARSE_PROVIDED_DEPENDENCY_NAMES_1_OF_APP_0, appName, envValue), e);
            return null;
        }
    }

    private static Map<String, Object> parseDeployAttributes(Map<String, String> appEnv) {
        String envValue = appEnv.get(Constants.ENV_DEPLOY_ATTRIBUTES);
        if (envValue == null) {
            return null;
        }
        return JsonUtil.convertJsonToMap(envValue);
    }

}

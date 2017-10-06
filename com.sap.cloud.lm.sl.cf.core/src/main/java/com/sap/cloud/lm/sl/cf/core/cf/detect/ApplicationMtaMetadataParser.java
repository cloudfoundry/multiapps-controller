package com.sap.cloud.lm.sl.cf.core.cf.detect;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.google.gson.reflect.TypeToken;
import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaMetadata;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.ListUtil;
import com.sap.cloud.lm.sl.mta.model.Version;

public class ApplicationMtaMetadataParser {

    public static ApplicationMtaMetadata parseAppMetadata(CloudApplication app) throws ParsingException {
        Map<String, String> appEnv = app.getEnvAsMap();
        DeployedMtaMetadata mtaMetadata = parseMtaMetadata(appEnv.get(Constants.ENV_MTA_METADATA));
        List<String> services = parseServices(appEnv.get(Constants.ENV_MTA_SERVICES));
        String moduleName = parseModuleName(appEnv.get(Constants.ENV_MTA_MODULE_METADATA));
        List<String> providedDependencyNames = parseProvidedDependencyNames(
            appEnv.get(Constants.ENV_MTA_MODULE_PUBLIC_PROVIDED_DEPENDENCIES));
        Map<String, Object> deployAttributes = parseDeployAttributes(appEnv.get(Constants.ENV_DEPLOY_ATTRIBUTES));

        if (mtaMetadata == null && services == null && moduleName == null && providedDependencyNames == null && deployAttributes == null) {
            return null;
        }
        return new ApplicationMtaMetadata(mtaMetadata, services, moduleName, providedDependencyNames, deployAttributes);
    }

    private static DeployedMtaMetadata parseMtaMetadata(String envValue) throws ParsingException {
        if (envValue == null) {
            return null;
        }
        Map<String, Object> mtaPropsMap = JsonUtil.convertJsonToMap(envValue);
        String id = (String) mtaPropsMap.get(Constants.ATTR_ID);
        String versionString = (String) mtaPropsMap.get(Constants.ATTR_VERSION);
        Version version = Version.parseVersion(versionString);
        return new DeployedMtaMetadata(id, version);
    }

    private static List<String> parseServices(String envValue) throws ParsingException {
        if (envValue == null) {
            return null;
        }
        return ListUtil.cast(JsonUtil.convertJsonToList(envValue));
    }

    private static String parseModuleName(String envValue) throws ParsingException {
        if (envValue == null) {
            return null;
        }
        Map<String, Object> mtaPropsMap = JsonUtil.convertJsonToMap(envValue);
        return (String) mtaPropsMap.get(Constants.ATTR_NAME);
    }

    private static List<String> parseProvidedDependencyNames(String envValue) {
        if (envValue == null) {
            return null;
        }
        try {
            return JsonUtil.convertJsonToList(envValue, new TypeToken<List<String>>() {
            }.getType());
        } catch (ParsingException e) {
            // The parsing fails PROBABLY because the provided dependencies are represented using
            // the old format.
            return null;
        }
    }

    private static Map<String, Object> parseDeployAttributes(String envValue) {
        if (envValue == null) {
            return null;
        }
        return JsonUtil.convertJsonToMap(envValue);
    }

}

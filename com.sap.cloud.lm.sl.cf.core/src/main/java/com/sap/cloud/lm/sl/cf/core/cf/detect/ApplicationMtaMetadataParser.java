package com.sap.cloud.lm.sl.cf.core.cf.detect;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.cf.core.model.ApplicationMtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.MtaMetadata;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaModule;
import com.sap.cloud.lm.sl.cf.core.model.DeployedMtaResource;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.Version;

public class ApplicationMtaMetadataParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationMtaMetadataParser.class);

    private ApplicationMtaMetadataParser() {
    }

    public static ApplicationMtaMetadata parseAppMetadata(CloudApplication app) {
        try {
            return attemptToParseAppMetadata(app);
        } catch (ParsingException e) {
            throw new ParsingException(e, Messages.CANT_PARSE_MTA_METADATA_FOR_APP_0, app.getName());
        }
    }

    private static ApplicationMtaMetadata attemptToParseAppMetadata(CloudApplication app) {
        Map<String, String> appEnv = app.getEnv();
        MtaMetadata mtaMetadata = parseMtaMetadata(app, appEnv);
        List<String> serviceNames = parseServices(appEnv);
        String moduleName = parseModuleName(app, appEnv);
        List<String> providedDependencyNames = parseProvidedDependencyNames(app.getName(), appEnv);

        List<Object> metadataFields = Arrays.asList(mtaMetadata, serviceNames, moduleName, providedDependencyNames);
        if (metadataFields.stream()
                          .allMatch(Objects::isNull)) {
            return null;
        }
        if (metadataFields.stream()
                          .anyMatch(Objects::isNull)) {
            throw new ParsingException(Messages.MTA_METADATA_FOR_APP_0_IS_INCOMPLETE, app.getName());
        }
        List<DeployedMtaResource> services = serviceNames.stream()
                                                         .map(name -> DeployedMtaResource.builder()
                                                                                      .withServiceName(name)
                                                                                      .build())
                                                         .collect(Collectors.toList());
        DeployedMtaModule module = DeployedMtaModule.builder()
                                                    .withAppName(app.getName())
                                                    .withModuleName(moduleName)
                                                    .withProvidedDependencyNames(providedDependencyNames)
                                                    .withServices(services)
                                                    .build();
        return ApplicationMtaMetadata.builder().withMtaMetadata(mtaMetadata).withModule(module).build();
    }

    private static MtaMetadata parseMtaMetadata(CloudApplication app, Map<String, String> appEnv) {
        String envValue = appEnv.get(Constants.ENV_MTA_METADATA);
        if (envValue == null) {
            return null;
        }
        Map<String, Object> mtaMetadata = JsonUtil.convertJsonToMap(envValue);
        return buildMtaMetadata(app, mtaMetadata);
    }

    private static MtaMetadata buildMtaMetadata(CloudApplication app, Map<String, Object> mtaMetadata) {
        String exceptionMessage = MessageFormat.format(Messages.ENV_OF_APP_0_CONTAINS_INVALID_VALUE_FOR_1, app.getName(),
                                                       Constants.ENV_MTA_METADATA);
        String id = (String) getRequired(mtaMetadata, Constants.ATTR_ID, exceptionMessage);
        String version = (String) getRequired(mtaMetadata, Constants.ATTR_VERSION, exceptionMessage);
        return new MtaMetadata(id, Version.parseVersion(version));
    }

    private static List<String> parseServices(Map<String, String> appEnv) {
        String envValue = appEnv.get(Constants.ENV_MTA_SERVICES);
        if (envValue == null) {
            return null;
        }
        return JsonUtil.convertJsonToList(envValue, new TypeReference<List<String>>() {
        });
    }

    private static String parseModuleName(CloudApplication app, Map<String, String> appEnv) {
        String envValue = appEnv.get(Constants.ENV_MTA_MODULE_METADATA);
        if (envValue == null) {
            return null;
        }
        Map<String, Object> mtaModuleMetadata = JsonUtil.convertJsonToMap(envValue);
        return (String) getRequired(mtaModuleMetadata, Constants.ATTR_NAME,
                                    MessageFormat.format(Messages.ENV_OF_APP_0_CONTAINS_INVALID_VALUE_FOR_1, app.getName(),
                                                         Constants.ENV_MTA_MODULE_METADATA));
    }

    private static List<String> parseProvidedDependencyNames(String appName, Map<String, String> appEnv) {
        String envValue = appEnv.get(Constants.ENV_MTA_MODULE_PUBLIC_PROVIDED_DEPENDENCIES);
        if (envValue == null) {
            return null;
        }
        try {
            return JsonUtil.convertJsonToList(envValue, new TypeReference<List<String>>() {
            });
        } catch (ParsingException e) {
            LOGGER.warn(MessageFormat.format(Messages.COULD_NOT_PARSE_PROVIDED_DEPENDENCY_NAMES_1_OF_APP_0, appName, envValue), e);
            return Collections.emptyList();
        }
    }

    private static <K, V> V getRequired(Map<K, V> map, K key, String exceptionMessage) {
        V value = map.get(key);
        if (value == null) {
            throw new ParsingException(exceptionMessage);
        }
        return value;
    }

}

package com.sap.cloud.lm.sl.cf.core.cf.metadata.processor;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.mta.model.Version;

public class BaseMtaMetadataParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseMtaMetadataParser.class);

    protected Version parseMtaVersion(String mtaVersion, String exceptionMessage) {
        try {
            return mtaVersion == null ? null : Version.parseVersion(mtaVersion);
        } catch (ParsingException e) {
            throw new ParsingException(e, exceptionMessage);
        }
    }

    protected String parseNameAttribute(Map<String, String> source, String key) {
        String mtaModule = source.get(key);
        return (String) JsonUtil.convertJsonToMap(mtaModule)
                                .get(Constants.ATTR_NAME);
    }

    protected List<String> parseModuleProvidedDependencies(String appName, Map<String, String> source, String key) {
        String moduleProvidedDependencies = source.get(key);
        try {
            return JsonUtil.convertJsonToList(moduleProvidedDependencies, new TypeReference<List<String>>() {
            });
        } catch (ParsingException e) {
            LOGGER.warn(MessageFormat.format(Messages.COULD_NOT_PARSE_PROVIDED_DEPENDENCY_NAMES_1_OF_APP_0, appName,
                                             moduleProvidedDependencies),
                        e);
            return Collections.emptyList();
        }
    }

    protected List<String> parseList(Map<String, String> source, String key) {
        String moduleBoundMtaServices = source.get(key);
        return JsonUtil.convertJsonToList(moduleBoundMtaServices, new TypeReference<List<String>>() {
        });
    }
}

package org.cloudfoundry.multiapps.controller.core.parser;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CfUserMetadata;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCfUserMetadata;
import org.cloudfoundry.multiapps.controller.core.Messages;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.core.validators.parameters.CfMetadataValidator;
import org.cloudfoundry.multiapps.mta.util.PropertiesUtil;

public class CfMetadataParser implements ParametersParser<CfUserMetadata> {

    private static final String LABELS_KEY = "labels";
    private static final String ANNOTATIONS_KEY = "annotations";

    @Override
    public CfUserMetadata parse(List<Map<String, Object>> parametersList) {
        Object rawValue = PropertiesUtil.getPropertyValue(parametersList, SupportedParameters.CF_METADATA, null);
        if (rawValue == null) {
            return ImmutableCfUserMetadata.builder()
                                          .build();
        }
        if (!(rawValue instanceof Map)) {
            throw new ContentException(Messages.CF_METADATA_INVALID_TYPE);
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> cfMetadataMap = (Map<String, Object>) rawValue;
        Map<String, String> labels = extractStringMap(cfMetadataMap, LABELS_KEY);
        Map<String, String> annotations = extractStringMap(cfMetadataMap, ANNOTATIONS_KEY);

        CfUserMetadata result = ImmutableCfUserMetadata.builder()
                                                       .labels(labels)
                                                       .annotations(annotations)
                                                       .build();
        new CfMetadataValidator().validate(result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractStringMap(Map<String, Object> parent, String key) {
        Object value = parent.get(key);
        if (value == null) {
            return Collections.emptyMap();
        }
        if (!(value instanceof Map)) {
            throw new ContentException(Messages.CF_METADATA_INVALID_TYPE);
        }
        Map<?, ?> rawMap = (Map<?, ?>) value;
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (!(entry.getKey() instanceof String) || (entry.getValue() != null && !(entry.getValue() instanceof String))) {
                throw new ContentException(Messages.CF_METADATA_INVALID_TYPE);
            }
        }
        return (Map<String, String>) rawMap;
    }

}

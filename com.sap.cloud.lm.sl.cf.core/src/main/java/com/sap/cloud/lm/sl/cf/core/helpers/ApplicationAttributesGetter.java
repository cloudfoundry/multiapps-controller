package com.sap.cloud.lm.sl.cf.core.helpers;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class ApplicationAttributesGetter {

    private Map<String, Object> attributes;
    private String appName;

    private ApplicationAttributesGetter(String appName, Map<String, Object> attributes) {
        this.attributes = attributes;
        this.appName = appName;
    }

    public <T> T getAttribute(String attributeName, Class<T> attributeType) {
        return getAttribute(attributeName, attributeType, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttribute(String attributeName, Class<T> attributeType, T defaultValue) {
        Object attributeValue = attributes.getOrDefault(attributeName, defaultValue);
        if (attributeValue != null && !attributeType.isInstance(attributeValue)) {
            throw new SLException(MessageFormat.format(Messages.ATTRIBUTE_0_OF_APP_1_IS_OF_TYPE_2_INSTEAD_OF_3, attributeName, appName,
                attributeValue.getClass()
                    .getName(),
                attributeType.getName()));
        }
        return (T) attributeValue;
    }

    public static ApplicationAttributesGetter forApplication(CloudApplication app) throws ParsingException {
        String attributes = app.getEnvAsMap()
            .get(com.sap.cloud.lm.sl.cf.core.Constants.ENV_DEPLOY_ATTRIBUTES);
        Map<String, Object> parsedAttributes = parseAttributes(attributes);
        return new ApplicationAttributesGetter(app.getName(), parsedAttributes);
    }

    private static Map<String, Object> parseAttributes(String attributesJson) {
        if (attributesJson == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> parsedAttributes = JsonUtil.convertJsonToMap(attributesJson);
        return parsedAttributes == null ? Collections.emptyMap() : parsedAttributes;
    }

}

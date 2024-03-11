package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.Collections;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudApplication;

import com.sap.cloud.lm.sl.cf.core.Constants;
import com.sap.cloud.lm.sl.cf.core.message.Messages;
import com.sap.cloud.lm.sl.common.ParsingException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class ApplicationAttributes {

    private final String appName;
    private final Map<String, Object> attributes;

    private ApplicationAttributes(String appName, Map<String, Object> attributes) {
        this.appName = appName;
        this.attributes = attributes;
    }

    public static ApplicationAttributes fromApplication(CloudApplication app) {
        Map<String, Object> attributes = parseAttributes(app);
        return new ApplicationAttributes(app.getName(), attributes);
    }

    private static Map<String, Object> parseAttributes(CloudApplication app) {
        Map<String, String> env = app.getEnvAsMap();
        Map<String, Object> attributes = parseAttributes(app, env.get(Constants.ENV_DEPLOY_ATTRIBUTES));
        return attributes == null ? Collections.emptyMap() : attributes;
    }

    private static Map<String, Object> parseAttributes(CloudApplication app, String attributesJson) {
        if (attributesJson == null) {
            return null;
        }
        try {
            return JsonUtil.convertJsonToMap(attributesJson);
        } catch (ParsingException e) {
            throw new ParsingException(e, Messages.COULD_NOT_PARSE_ATTRIBUTES_OF_APP_0, app.getName());
        }
    }

    public <T> T get(String attributeName, Class<T> attributeClass) {
        return get(attributeName, attributeClass, null);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String attributeName, Class<T> attributeClass, T defaultValue) {
        Object attribute = attributes.getOrDefault(attributeName, defaultValue);
        if (!hasCorrectType(attribute, attributeClass)) {
            Class<?> actualAttributeClass = attribute.getClass();
            throw new ParsingException(Messages.ATTRIBUTE_0_OF_APP_1_IS_OF_TYPE_2_INSTEAD_OF_3,
                                       attributeName,
                                       appName,
                                       actualAttributeClass.getSimpleName(),
                                       attributeClass.getSimpleName());
        }
        return (T) attribute;
    }

    private boolean hasCorrectType(Object attribute, Class<?> expectedType) {
        return attribute == null || expectedType.isInstance(attribute);
    }

}

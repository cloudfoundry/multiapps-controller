package com.sap.cloud.lm.sl.cf.core.security.serialization;

import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class SecureJsonSerializer extends SecureSerializer<JsonElement<Object>> {

    public SecureJsonSerializer(SecureSerializerConfiguration configuration) {
        super(configuration);
    }

    @Override
    protected JsonElement<Object> toTree(Object object) {
        Object tree = JsonUtil.getObjectMapper()
                              .convertValue(object, Object.class);
        return new JsonElement<>("", "", tree);
    }

    @Override
    protected String toString(JsonElement<Object> element) {
        return JsonUtil.toJson(element.getObject(), configuration.formattedOutputIsEnabled());
    }

}

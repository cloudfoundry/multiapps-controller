package com.sap.cloud.lm.sl.cf.core.security.serialization;

import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class SecureJsonSerializer extends SecureSerializer {

    public SecureJsonSerializer(SecureSerializerConfiguration configuration) {
        super(configuration);
    }

    @Override
    protected Object toTree(Object object) {
        return JsonUtil.getObjectMapper()
                       .convertValue(object, Object.class);
    }

    @Override
    protected String serializeTree(Object object) {
        return JsonUtil.toJson(object, configuration.formattedOutputIsEnabled());
    }

}

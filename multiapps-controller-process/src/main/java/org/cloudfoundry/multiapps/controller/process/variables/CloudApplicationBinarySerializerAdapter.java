package org.cloudfoundry.multiapps.controller.process.variables;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.common.util.JsonUtil;

public class CloudApplicationBinarySerializerAdapter implements Serializer<CloudApplication> {

    @Override
    public Object serialize(CloudApplication value) {
        return JsonUtil.toJsonBinary(value);
    }

    @Override
    public CloudApplication deserialize(Object serializedValue) {
        Map<String, Object> appMap = JsonUtil.fromJsonBinary((byte[]) serializedValue, new TypeReference<>() {
        });
        if (appMap.containsKey("lifecycle")) {
            return JsonUtil.fromJsonBinary((byte[]) serializedValue, CloudApplication.class);
        }
        var adapter = new CloudApplicationSerializerAdapter();
        return adapter.createApp(appMap);
    }
}

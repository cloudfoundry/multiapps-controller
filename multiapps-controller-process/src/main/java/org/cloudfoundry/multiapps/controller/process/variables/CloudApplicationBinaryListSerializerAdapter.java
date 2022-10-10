package org.cloudfoundry.multiapps.controller.process.variables;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.common.util.JsonUtil;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CloudApplicationBinaryListSerializerAdapter implements Serializer<List<CloudApplication>> {

    @Override
    public Object serialize(List<CloudApplication> value) {
        return JsonUtil.toJsonBinary(value);
    }

    @Override
    public List<CloudApplication> deserialize(Object serializedValue) {
        List<Map<String, Object>> appMaps = JsonUtil.fromJsonBinary((byte[]) serializedValue, new TypeReference<>() {
        });
        return appMaps.stream()
                      .map(this::resolve)
                      .collect(Collectors.toList());
    }

    private CloudApplication resolve(Map<String, Object> appMap) {
        if (appMap.containsKey("lifecycle")) {
            String serializedValue = JsonUtil.toJson(appMap);
            return JsonUtil.fromJson(serializedValue, CloudApplication.class);
        }
        var adapter = new CloudApplicationSerializerAdapter();
        return adapter.createApp(appMap);
    }
}

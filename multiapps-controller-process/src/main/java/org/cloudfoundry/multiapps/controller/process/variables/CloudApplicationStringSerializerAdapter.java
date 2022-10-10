package org.cloudfoundry.multiapps.controller.process.variables;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.common.util.JsonUtil;

import java.util.Map;

public class CloudApplicationStringSerializerAdapter implements Serializer<CloudApplication> {

    @Override
    public Object serialize(CloudApplication value) {
        return JsonUtil.toJson(value);
    }

    @Override
    public CloudApplication deserialize(Object serializedValue) {
        Map<String, Object> appMap = JsonUtil.fromJson((String) serializedValue, new TypeReference<>() {
        });
        if (appMap.containsKey("lifecycle")) {
            return JsonUtil.fromJson((String) serializedValue, CloudApplication.class);
        }
        var adapter = new CloudApplicationSerializerAdapter();
        return adapter.createApp(appMap);
    }
}

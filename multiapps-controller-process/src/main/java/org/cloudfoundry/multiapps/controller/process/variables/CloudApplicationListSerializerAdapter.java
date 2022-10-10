package org.cloudfoundry.multiapps.controller.process.variables;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import org.cloudfoundry.multiapps.common.util.JsonUtil;

import java.util.List;
import java.util.stream.Collectors;

public class CloudApplicationListSerializerAdapter implements Serializer<List<CloudApplication>> {

    private final CloudApplicationStringSerializerAdapter singularSerializer = new CloudApplicationStringSerializerAdapter();

    @Override
    public Object serialize(List<CloudApplication> values) {
        return values.stream()
                     .map(JsonUtil::toJson)
                     .collect(Collectors.toList());
    }

    @Override
    public List<CloudApplication> deserialize(Object serializedValue) {
        @SuppressWarnings("unchecked")
        List<String> serializedValues = (List<String>) serializedValue;
        return serializedValues.stream()
                               .map(singularSerializer::deserialize)
                               .collect(Collectors.toList());
    }

}

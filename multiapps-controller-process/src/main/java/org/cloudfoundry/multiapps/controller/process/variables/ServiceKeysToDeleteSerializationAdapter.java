package org.cloudfoundry.multiapps.controller.process.variables;

import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.core.model.DeployedMtaServiceKey;

import com.fasterxml.jackson.core.type.TypeReference;
/**
 * 
 * Needed for backwards compatibility due to changes in how this parameter was serialized between versions -
 * from binary to a StringList currently. This class should be removed in a following release.
 *
 */
public class ServiceKeysToDeleteSerializationAdapter implements Serializer<List<DeployedMtaServiceKey>> {

    public static TypeReference<List<DeployedMtaServiceKey>> SERVICE_KEYS_LIST_TYPE_REFERENCE = new TypeReference<>() {
    };
    public static TypeReference<DeployedMtaServiceKey> SERVICE_KEY_ELEMENT_TYPE_REFERENCE = new TypeReference<>() {
    };

    @Override
    public Object serialize(List<DeployedMtaServiceKey> value) {
        return value.stream()
                    .map(JsonUtil::toJson)
                    .collect(Collectors.toList());
    }

    @Override
    public List<DeployedMtaServiceKey> deserialize(Object serializedValue) {
        if (serializedValue instanceof List<?>) {
            List<String> serializedValues = (List<String>) serializedValue;
            return serializedValues.stream()
                                   .map(value -> JsonUtil.fromJson(value, SERVICE_KEY_ELEMENT_TYPE_REFERENCE))
                                   .collect(Collectors.toList());
        } else {
            return JsonUtil.fromJsonBinary((byte[]) serializedValue, SERVICE_KEYS_LIST_TYPE_REFERENCE);
        }
    }

}

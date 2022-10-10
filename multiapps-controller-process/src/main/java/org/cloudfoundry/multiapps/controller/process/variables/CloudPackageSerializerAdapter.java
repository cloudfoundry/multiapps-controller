package org.cloudfoundry.multiapps.controller.process.variables;

import com.fasterxml.jackson.core.type.TypeReference;
import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.CloudPackage;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudPackage;
import com.sap.cloudfoundry.client.facade.domain.ImmutableDockerData;
import com.sap.cloudfoundry.client.facade.domain.Status;
import org.cloudfoundry.multiapps.common.util.JsonUtil;

import java.util.Collections;
import java.util.Map;

public class CloudPackageSerializerAdapter implements Serializer<CloudPackage> {

    @Override
    public Object serialize(CloudPackage value) {
        return JsonUtil.toJson(value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CloudPackage deserialize(Object serializedValue) {
        Map<String, Object> rawData = JsonUtil.fromJson((String) serializedValue, new TypeReference<>() {
        });
        var typeValue = (String) rawData.get("type");
        var type = typeValue == null ? CloudPackage.Type.BITS : CloudPackage.Type.valueOf(typeValue.toUpperCase());
        if (type == CloudPackage.Type.BITS) {
            rawData.put("type", CloudPackage.Type.BITS.toString()); //ensure that the value is lowercase
            //in the case of a "bits" package, the json structure is the same as before
            return deserializeObject(rawData, CloudPackage.class);
        }

        var metadata = deserializeObject(rawData.get("metadata"), CloudMetadata.class);
        var statusValue = (String) rawData.get("status");
        var status = statusValue == null ? Status.AWAITING_UPLOAD : Status.valueOf(statusValue);
        var data = (Map<String, Object>) rawData.getOrDefault("data", Collections.emptyMap());
        var dockerImage = (String) data.get("image");
        var dockerUsername = (String) data.get("username");
        var dockerPassword = (String) data.get("password");
        return ImmutableCloudPackage.builder()
                                    .data(ImmutableDockerData.builder()
                                                             .image(dockerImage)
                                                             .username(dockerUsername)
                                                             .password(dockerPassword)
                                                             .build())
                                    .metadata(metadata)
                                    .type(CloudPackage.Type.DOCKER)
                                    .status(status)
                                    .build();
    }

    private <T> T deserializeObject(Object value, Class<T> clazz) {
        String serializedValue = JsonUtil.toJson(value);
        return JsonUtil.fromJson(serializedValue, clazz);
    }
}

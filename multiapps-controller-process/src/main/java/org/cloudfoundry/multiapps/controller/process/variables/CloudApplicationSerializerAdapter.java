package org.cloudfoundry.multiapps.controller.process.variables;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.domain.Lifecycle;
import com.sap.cloudfoundry.client.facade.domain.LifecycleType;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudApplication;
import com.sap.cloudfoundry.client.facade.domain.ImmutableLifecycle;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.multiapps.common.util.JsonUtil;

//TODO delete this, the rest of CloudApplication adapters and the CloudPackage adapter after 1-2 takts from 10a have passed
public class CloudApplicationSerializerAdapter {

    @SuppressWarnings("unchecked")
    public CloudApplication createApp(Map<String, Object> appMap) {
        var name = (String) appMap.get("name");
        var metadata = deserializeObject(appMap.get("metadata"), CloudMetadata.class);
        var v3metadata = deserializeObject(appMap.get("v3Metadata"), Metadata.class);
        var state = (String) appMap.get("state");
        var space = deserializeObject(appMap.get("space"), CloudSpace.class);
        var staging = (Map<String, Object>) appMap.getOrDefault("staging", Collections.emptyMap());
        var dockerInfo = staging.get("dockerInfo");
        Lifecycle lifecycle;
        if (dockerInfo != null) {
            lifecycle = ImmutableLifecycle.builder()
                                          .type(LifecycleType.DOCKER)
                                          .data(Map.of())
                                          .build();
        } else {
            var buildpacks = (List<String>) staging.getOrDefault("buildpacks", Collections.emptyList());
            var stack = (String) staging.getOrDefault("stackName", "");
            lifecycle = ImmutableLifecycle.builder()
                                          .type(LifecycleType.BUILDPACK)
                                          .data(Map.of("buildpacks", buildpacks, "stack", stack))
                                          .build();
        }
        return ImmutableCloudApplication.builder()
                                        .name(name)
                                        .metadata(metadata)
                                        .v3Metadata(v3metadata)
                                        .space(space)
                                        .state(state == null ? CloudApplication.State.STOPPED : CloudApplication.State.valueOf(state))
                                        .lifecycle(lifecycle)
                                        .build();
    }

    private <T> T deserializeObject(Object value, Class<T> clazz) {
        String serializedValue = JsonUtil.toJson(value);
        return JsonUtil.fromJson(serializedValue, clazz);
    }
}

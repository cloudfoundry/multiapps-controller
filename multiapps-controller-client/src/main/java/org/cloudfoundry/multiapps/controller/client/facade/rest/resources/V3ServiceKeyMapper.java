package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableServiceCredentialBindingOperation;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceCredentialBindingOperation;

public final class V3ServiceKeyMapper {

    private V3ServiceKeyMapper() {
    }

    public static CloudServiceKey toCloudServiceKey(V3ServiceBinding key, CloudServiceInstance serviceInstance) {
        return toCloudServiceKey(key, serviceInstance, null);
    }

    public static CloudServiceKey toCloudServiceKey(V3ServiceBinding key, CloudServiceInstance serviceInstance,
                                                    Map<String, Object> credentials) {
        return ImmutableCloudServiceKey.builder()
                                       .metadata(V3ResourceMappers.parseMetadata(key.guid(), key.createdAt(), key.updatedAt()))
                                       .v3Metadata(V3ResourceMappers.toV3Metadata(key.metadata()))
                                       .name(key.name())
                                       .credentials(credentials)
                                       .serviceInstance(serviceInstance)
                                       .serviceKeyOperation(parseServiceKeyOperation(key.lastOperation()))
                                       .build();
    }

    private static ServiceCredentialBindingOperation parseServiceKeyOperation(V3ServiceBinding.V3LastOperation lastOperation) {
        if (lastOperation == null) {
            return null;
        }

        return ImmutableServiceCredentialBindingOperation.builder()
                                                         .type(ServiceCredentialBindingOperation.Type.fromString(lastOperation.type()))
                                                         .state(ServiceCredentialBindingOperation.State.fromString(lastOperation.state()))
                                                         .description(lastOperation.description())
                                                         .createdAt(V3ResourceMappers.parseNullableDate(lastOperation.createdAt()))
                                                         .updatedAt(V3ResourceMappers.parseNullableDate(lastOperation.updatedAt()))
                                                         .build();
    }

}

package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableServiceCredentialBindingOperation;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceCredentialBindingOperation;

/**
 * Maps a CF v3 service credential binding of {@code type=key} ({@link V3ServiceBinding}) to the project's {@link CloudServiceKey} domain
 * object. Mirrors the OSS {@code RawCloudServiceKey#derive()} adapter field-for-field, so both client implementations yield identical
 * domain objects:
 * <ul>
 * <li>{@code metadata} — from the binding resource envelope ({@code guid}, {@code created_at}, {@code updated_at});</li>
 * <li>{@code v3Metadata} — labels/annotations from {@code metadata};</li>
 * <li>{@code name} — the service key name;</li>
 * <li>{@code credentials} — nullable; fetched separately from the binding <em>details</em> endpoint (absent when listing without
 * credentials);</li>
 * <li>{@code serviceInstance} — the owning (already-derived) {@link CloudServiceInstance};</li>
 * <li>{@code serviceKeyOperation} — from {@code last_operation}, mirroring {@link ServiceCredentialBindingOperation#from}.</li>
 * </ul>
 */
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

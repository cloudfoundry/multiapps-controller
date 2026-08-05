package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceInstanceType;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceOperation;

/**
 * Maps the {@link V3ServiceInstance} wire model to the project's {@link CloudServiceInstance} domain object. Mirrors the OSS
 * {@code RawCloudServiceInstance} and {@code RawV3CloudServiceInstance} adapters field-for-field, so both client implementations yield
 * identical domain objects.
 *
 * <p>
 * The OSS {@code RawCloudServiceInstance} derives {@code plan} and {@code label} from auxiliary {@code ServicePlan} /
 * {@code ServiceOffering} resources it is zipped with; those names are resolved by the operations helper and passed in here.
 * {@code RawV3CloudServiceInstance} (used for the {@code byNames} batch lookups) maps only the base fields with no auxiliary content.
 * </p>
 */
public final class V3ServiceInstanceMapper {

    private V3ServiceInstanceMapper() {
    }

    /**
     * Full mapping mirroring the OSS {@code RawCloudServiceInstance.derive()}: base fields plus the resolved plan/offering names.
     */
    public static CloudServiceInstance toCloudServiceInstance(V3ServiceInstance serviceInstance, String planName, String labelName) {
        return ImmutableCloudServiceInstance.builder()
                                            .metadata(V3ResourceMappers.parseMetadata(serviceInstance.guid(),
                                                                                      serviceInstance.createdAt(),
                                                                                      serviceInstance.updatedAt()))
                                            .v3Metadata(V3ResourceMappers.toV3Metadata(serviceInstance.metadata()))
                                            .name(serviceInstance.name())
                                            .plan(planName)
                                            .label(labelName)
                                            .type(parseType(serviceInstance.type()))
                                            .tags(serviceInstance.tags() == null ? java.util.Collections.emptyList()
                                                : serviceInstance.tags())
                                            .lastOperation(parseLastOperation(serviceInstance.lastOperation()))
                                            .syslogDrainUrl(serviceInstance.syslogDrainUrl())
                                            .build();
    }

    /**
     * Base-only mapping mirroring the OSS {@code RawV3CloudServiceInstance.derive()}: metadata, v3Metadata, name and tags (used by the
     * {@code byNames} batch lookups, which never resolve plan/offering and leave {@code type} unset).
     */
    public static CloudServiceInstance toCloudServiceInstanceWithoutAuxiliaryContent(V3ServiceInstance serviceInstance) {
        return ImmutableCloudServiceInstance.builder()
                                            .metadata(V3ResourceMappers.parseMetadata(serviceInstance.guid(),
                                                                                      serviceInstance.createdAt(),
                                                                                      serviceInstance.updatedAt()))
                                            .v3Metadata(V3ResourceMappers.toV3Metadata(serviceInstance.metadata()))
                                            .name(serviceInstance.name())
                                            .tags(serviceInstance.tags() == null ? java.util.Collections.emptyList()
                                                : serviceInstance.tags())
                                            .build();
    }

    /**
     * Map the CF v3 {@code type} JSON value ({@code managed} / {@code user-provided}) to the project's {@link ServiceInstanceType}.
     */
    private static ServiceInstanceType parseType(String type) {
        if (type == null) {
            return null;
        }
        for (ServiceInstanceType serviceInstanceType : ServiceInstanceType.values()) {
            if (serviceInstanceType.getValue()
                                   .equals(type)) {
                return serviceInstanceType;
            }
        }
        return null;
    }

    /**
     * Mirrors {@code ServiceOperation.fromLastOperation(LastOperation)}: a {@code null} operation, type or state yields {@code null}.
     */
    private static ServiceOperation parseLastOperation(V3ServiceInstance.V3LastOperation lastOperation) {
        if (lastOperation == null || lastOperation.type() == null || lastOperation.state() == null) {
            return null;
        }
        ServiceOperation.Type type = ServiceOperation.Type.fromString(lastOperation.type());
        ServiceOperation.State state = ServiceOperation.State.fromString(lastOperation.state());
        return new ServiceOperation(type, lastOperation.description(), state);
    }

}

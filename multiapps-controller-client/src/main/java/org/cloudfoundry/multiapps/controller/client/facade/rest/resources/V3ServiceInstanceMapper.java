package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceInstanceType;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceOperation;

public final class V3ServiceInstanceMapper {

    private V3ServiceInstanceMapper() {
    }

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

    private static ServiceOperation parseLastOperation(V3ServiceInstance.V3LastOperation lastOperation) {
        if (lastOperation == null || lastOperation.type() == null || lastOperation.state() == null) {
            return null;
        }

        ServiceOperation.Type type = ServiceOperation.Type.fromString(lastOperation.type());
        ServiceOperation.State state = ServiceOperation.State.fromString(lastOperation.state());

        return new ServiceOperation(type, lastOperation.description(), state);
    }

}

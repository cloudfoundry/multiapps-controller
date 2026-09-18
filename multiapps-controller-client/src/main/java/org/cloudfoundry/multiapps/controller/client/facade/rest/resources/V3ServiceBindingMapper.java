package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableServiceCredentialBindingOperation;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceCredentialBindingOperation;

public final class V3ServiceBindingMapper {

    private V3ServiceBindingMapper() {
    }

    public static CloudServiceBinding toCloudServiceBinding(V3ServiceBinding binding) {
        return ImmutableCloudServiceBinding.builder()
                                           .metadata(V3ResourceMappers.parseMetadata(binding.guid(), binding.createdAt(),
                                                                                     binding.updatedAt()))
                                           .applicationGuid(parseApplicationGuid(binding))
                                           .serviceInstanceGuid(parseServiceInstanceGuid(binding))
                                           .serviceBindingOperation(parseServiceBindingOperation(binding.lastOperation()))
                                           .build();
    }

    private static UUID parseApplicationGuid(V3ServiceBinding binding) {
        V3ServiceBinding.V3ToOneRelationship application = binding.relationships() == null ? null
            : binding.relationships()
                     .application();

        if (application == null || application.data() == null) {
            return null;
        }

        return V3ResourceMappers.parseNullableGuid(application.data()
                                                              .guid());
    }

    private static UUID parseServiceInstanceGuid(V3ServiceBinding binding) {
        return UUID.fromString(binding.relationships()
                                      .serviceInstance()
                                      .data()
                                      .guid());
    }

    private static ServiceCredentialBindingOperation parseServiceBindingOperation(V3ServiceBinding.V3LastOperation lastOperation) {
        if (lastOperation == null) {
            return null;
        }

        return ImmutableServiceCredentialBindingOperation.builder()
                                                         .type(ServiceCredentialBindingOperation.Type.fromString(lastOperation.type()))
                                                         .state(ServiceCredentialBindingOperation.State.fromString(lastOperation.state()))
                                                         .description(lastOperation.description())
                                                         .createdAt(parseDate(lastOperation.createdAt()))
                                                         .updatedAt(parseDate(lastOperation.updatedAt()))
                                                         .build();
    }

    private static LocalDateTime parseDate(String date) {
        return date == null ? null : LocalDateTime.parse(date, DateTimeFormatter.ISO_DATE_TIME);
    }

}

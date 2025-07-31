package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.util.UUID;

import org.cloudfoundry.client.v3.servicebindings.ServiceBinding;
import org.cloudfoundry.client.v3.servicebindings.ServiceBindingResource;
import org.immutables.value.Value;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceBinding;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceCredentialBindingOperation;

@Value.Immutable
public abstract class RawCloudServiceBinding extends RawCloudEntity<CloudServiceBinding> {

    @Value.Parameter
    public abstract ServiceBindingResource getServiceBinding();

    @Override
    public CloudServiceBinding derive() {
        ServiceBinding serviceBinding = getServiceBinding();
        var appRelationship = serviceBinding.getRelationships()
                                            .getApplication();
        return ImmutableCloudServiceBinding.builder()
                                           .metadata(parseResourceMetadata(serviceBinding))
                                           .applicationGuid(parseNullableGuid(appRelationship == null ? null
                                               : appRelationship.getData()
                                                                .getId()))
                                           .serviceInstanceGuid(UUID.fromString(serviceBinding.getRelationships()
                                                                                              .getServiceInstance()
                                                                                              .getData()
                                                                                              .getId()))
                                           .serviceBindingOperation(ServiceCredentialBindingOperation.from(getServiceBinding().getLastOperation()))
                                           .build();
    }

}

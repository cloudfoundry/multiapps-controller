package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.util.Map;

import org.cloudfoundry.AllowNulls;
import org.cloudfoundry.client.v3.servicebindings.ServiceBindingResource;
import org.immutables.value.Value;

import org.cloudfoundry.multiapps.controller.client.facade.Nullable;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Derivable;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceKey;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceCredentialBindingOperation;

@Value.Immutable
public abstract class RawCloudServiceKey extends RawCloudEntity<CloudServiceKey> {

    public abstract ServiceBindingResource getServiceBindingResource();

    @Nullable
    @AllowNulls
    public abstract Map<String, Object> getCredentials();

    public abstract Derivable<CloudServiceInstance> getServiceInstance();

    @Override
    public CloudServiceKey derive() {
        ServiceBindingResource serviceBindingResource = getServiceBindingResource();
        Derivable<CloudServiceInstance> serviceInstance = getServiceInstance();
        return ImmutableCloudServiceKey.builder()
                                       .metadata(parseResourceMetadata(serviceBindingResource))
                                       .v3Metadata(serviceBindingResource.getMetadata())
                                       .name(serviceBindingResource.getName())
                                       .credentials(getCredentials())
                                       .serviceInstance(serviceInstance.derive())
                                       .serviceKeyOperation(ServiceCredentialBindingOperation.from(serviceBindingResource.getLastOperation()))
                                       .build();
    }

}

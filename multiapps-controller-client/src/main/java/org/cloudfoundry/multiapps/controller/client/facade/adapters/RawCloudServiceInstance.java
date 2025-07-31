package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.util.Optional;

import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceResource;
import org.cloudfoundry.client.v3.serviceofferings.ServiceOffering;
import org.cloudfoundry.client.v3.serviceplans.ServicePlan;
import org.immutables.value.Value;

import org.cloudfoundry.multiapps.controller.client.facade.Nullable;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceOperation;

@Value.Immutable
public abstract class RawCloudServiceInstance extends RawCloudEntity<CloudServiceInstance> {

    @Value.Parameter
    public abstract ServiceInstanceResource getResource();

    @Nullable
    public abstract ServicePlan getServicePlan();

    @Nullable
    public abstract ServiceOffering getServiceOffering();

    @Override
    public CloudServiceInstance derive() {
        ServiceInstanceResource resource = getResource();
        return ImmutableCloudServiceInstance.builder()
                                            .metadata(parseResourceMetadata(resource))
                                            .v3Metadata(resource.getMetadata())
                                            .name(resource.getName())
                                            .plan(getServicePlanName())
                                            .label(getLabelName())
                                            .type(resource.getType())
                                            .tags(resource.getTags())
                                            .lastOperation(ServiceOperation.fromLastOperation(resource.getLastOperation()))
                                            .syslogDrainUrl(resource.getSyslogDrainUrl())
                                            .build();
    }

    private String getServicePlanName() {
        return Optional.ofNullable(getServicePlan())
                       .map(ServicePlan::getName)
                       .orElse(null);
    }

    private String getLabelName() {
        return Optional.ofNullable(getServiceOffering())
                       .map(ServiceOffering::getName)
                       .orElse(null);
    }

}

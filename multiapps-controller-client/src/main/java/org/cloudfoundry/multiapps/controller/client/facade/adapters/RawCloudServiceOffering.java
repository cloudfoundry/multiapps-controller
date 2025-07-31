package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.util.List;

import org.cloudfoundry.client.v3.serviceofferings.ServiceOfferingResource;
import org.immutables.value.Value;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceOffering;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServicePlan;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Derivable;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceOffering;

@Value.Immutable
public abstract class RawCloudServiceOffering extends RawCloudEntity<CloudServiceOffering> {

    public abstract ServiceOfferingResource getServiceOffering();

    public abstract List<Derivable<CloudServicePlan>> getServicePlans();

    @Override
    public CloudServiceOffering derive() {
        ServiceOfferingResource serviceOffering = getServiceOffering();
        return ImmutableCloudServiceOffering.builder()
                                            .metadata(parseResourceMetadata(serviceOffering))
                                            .name(serviceOffering.getName())
                                            .isAvailable(serviceOffering.getAvailable())
                                            .isBindable(serviceOffering.getBrokerCatalog()
                                                                       .getFeatures()
                                                                       .getBindable())
                                            .description(serviceOffering.getDescription())
                                            .isShareable(serviceOffering.getShareable())
                                            .extra(serviceOffering.getBrokerCatalog()
                                                                  .getMetadata())
                                            .docUrl(serviceOffering.getDocumentationUrl())
                                            .brokerId(serviceOffering.getRelationships()
                                                                     .getServiceBroker()
                                                                     .getData()
                                                                     .getId())
                                            .uniqueId(serviceOffering.getBrokerCatalog()
                                                                     .getBrokerCatalogId())
                                            .servicePlans(derive(getServicePlans()))
                                            .build();
    }

}

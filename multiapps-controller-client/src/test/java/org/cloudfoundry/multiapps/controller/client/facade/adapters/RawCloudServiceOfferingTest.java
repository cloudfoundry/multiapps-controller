package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.serviceofferings.BrokerCatalog;
import org.cloudfoundry.client.v3.serviceofferings.Features;
import org.cloudfoundry.client.v3.serviceofferings.ServiceOfferingRelationships;
import org.cloudfoundry.client.v3.serviceofferings.ServiceOfferingResource;
import org.junit.jupiter.api.Test;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceOffering;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServicePlan;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceOffering;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServicePlan;

public class RawCloudServiceOfferingTest {

    private static final String NAME = "postgresql";
    static final boolean AVAILABLE = true;
    static final boolean BINDABLE = true;
    static final boolean ALLOW_CONTEXT_UPDATES = false;
    static final boolean BINDINGS_RETRIEVABLE = true;
    static final boolean INSTANCES_RETRIEVABLE = true;
    static final boolean PLAN_UPDATEABLE = false;
    static final boolean SHAREABLE = true;
    static final String DESCRIPTION = "description";
    static final String DOCUMENTATION_URL = "/documentation";
    static final String UNIQUE_ID = "unique-id";
    static final String SERVICE_BROKER_GUID = UUID.randomUUID()
                                                  .toString();
    private static final Map<String, Object> EXTRA = Map.of("key-metadata", "value-metadata");
    private static final List<CloudServicePlan> PLANS = buildTestServiceBindings();

    @Test
    void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedServiceOffering(), buildRawServiceOffering());
    }

    private static CloudServiceOffering buildExpectedServiceOffering() {
        return ImmutableCloudServiceOffering.builder()
                                            .metadata(RawCloudEntityTest.EXPECTED_METADATA_PARSED_FROM_V3_RESOURCE)
                                            .brokerId(SERVICE_BROKER_GUID)
                                            .name(NAME)
                                            .isAvailable(AVAILABLE)
                                            .isBindable(BINDABLE)
                                            .isShareable(SHAREABLE)
                                            .description(DESCRIPTION)
                                            .extra(EXTRA)
                                            .docUrl(DOCUMENTATION_URL)
                                            .uniqueId(UNIQUE_ID)
                                            .servicePlans(PLANS)
                                            .build();
    }

    private static RawCloudServiceOffering buildRawServiceOffering() {
        return ImmutableRawCloudServiceOffering.builder()
                                               .serviceOffering(buildTestServiceOffering())
                                               .servicePlans(PLANS)
                                               .build();
    }

    public static ServiceOfferingResource buildTestServiceOffering() {
        return ServiceOfferingResource.builder()
                                      .id(RawCloudEntityTest.GUID_STRING)
                                      .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                                      .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                                      .available(AVAILABLE)
                                      .name(NAME)
                                      .brokerCatalog(BrokerCatalog.builder()
                                                                  .brokerCatalogId(UNIQUE_ID)
                                                                  .features(Features.builder()
                                                                                    .bindable(BINDABLE)
                                                                                    .allowContextUpdates(ALLOW_CONTEXT_UPDATES)
                                                                                    .bindingsRetrievable(BINDINGS_RETRIEVABLE)
                                                                                    .instancesRetrievable(INSTANCES_RETRIEVABLE)
                                                                                    .planUpdateable(PLAN_UPDATEABLE)
                                                                                    .build())
                                                                  .metadata(EXTRA)
                                                                  .build())
                                      .relationships(ServiceOfferingRelationships.builder()
                                                                                 .serviceBroker(ToOneRelationship.builder()
                                                                                                                 .data(Relationship.builder()
                                                                                                                                   .id(SERVICE_BROKER_GUID)
                                                                                                                                   .build())
                                                                                                                 .build())
                                                                                 .build())
                                      .shareable(SHAREABLE)
                                      .description(DESCRIPTION)
                                      .documentationUrl(DOCUMENTATION_URL)
                                      .build();
    }

    private static List<CloudServicePlan> buildTestServiceBindings() {
        return List.of(buildTestServiceBinding());
    }

    private static CloudServicePlan buildTestServiceBinding() {
        return ImmutableCloudServicePlan.builder()
                                        .name("v9.4-small")
                                        .build();
    }

}

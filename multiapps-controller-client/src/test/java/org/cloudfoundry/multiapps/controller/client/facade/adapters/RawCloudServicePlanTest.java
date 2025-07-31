package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.serviceplans.BrokerCatalog;
import org.cloudfoundry.client.v3.serviceplans.Features;
import org.cloudfoundry.client.v3.serviceplans.Schema;
import org.cloudfoundry.client.v3.serviceplans.Schemas;
import org.cloudfoundry.client.v3.serviceplans.ServiceInstanceSchema;
import org.cloudfoundry.client.v3.serviceplans.ServicePlan;
import org.cloudfoundry.client.v3.serviceplans.ServicePlanRelationships;
import org.cloudfoundry.client.v3.serviceplans.ServicePlanResource;
import org.cloudfoundry.client.v3.serviceplans.Visibility;
import org.junit.jupiter.api.Test;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServicePlan;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServicePlan;

public class RawCloudServicePlanTest {

    private static final String NAME = "v9.4-small";
    private static final String DESCRIPTION = "description";
    private static final Map<String, Object> EXTRA = Map.of("extra", "value");
    private static final String UNIQUE_ID = "unique-id";
    private static final boolean PUBLIC = true;
    private static final boolean FREE = false;
    private static final boolean PLAN_BINDABLE = true;
    private static final boolean PLAN_UPDATABLE = true;
    private static final boolean AVAILABLE = true;
    private static final String SERVICE_OFFERING_ID = UUID.randomUUID()
                                                          .toString();

    @Test
    void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedPlan(), buildRawServicePlan());
    }

    private static CloudServicePlan buildExpectedPlan() {
        return ImmutableCloudServicePlan.builder()
                                        .metadata(RawCloudEntityTest.EXPECTED_METADATA_PARSED_FROM_V3_RESOURCE)
                                        .name(NAME)
                                        .description(DESCRIPTION)
                                        .extra(EXTRA)
                                        .uniqueId(UNIQUE_ID)
                                        .isPublic(PUBLIC)
                                        .isFree(FREE)
                                        .serviceOfferingId(SERVICE_OFFERING_ID)
                                        .build();
    }

    private static RawCloudServicePlan buildRawServicePlan() {
        return ImmutableRawCloudServicePlan.of(buildTestServicePlan(NAME));
    }

    public static ServicePlan buildTestServicePlan(String planName) {
        return ServicePlanResource.builder()
                                  .id(RawCloudEntityTest.GUID_STRING)
                                  .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                                  .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                                  .available(AVAILABLE)
                                  .name(planName)
                                  .description(DESCRIPTION)
                                  .brokerCatalog(BrokerCatalog.builder()
                                                              .metadata(EXTRA)
                                                              .brokerCatalogId(UNIQUE_ID)
                                                              .features(Features.builder()
                                                                                .bindable(PLAN_BINDABLE)
                                                                                .planUpdateable(PLAN_UPDATABLE)
                                                                                .build())
                                                              .build())
                                  .visibilityType(Visibility.PUBLIC)
                                  .free(FREE)
                                  .relationships(ServicePlanRelationships.builder()
                                                                         .serviceOffering(ToOneRelationship.builder()
                                                                                                           .data(Relationship.builder()
                                                                                                                             .id(SERVICE_OFFERING_ID)
                                                                                                                             .build())
                                                                                                           .build())
                                                                         .build())
                                  .schemas(Schemas.builder()
                                                  .serviceInstance(ServiceInstanceSchema.builder()
                                                                                        .create(Schema.builder()
                                                                                                      .build())
                                                                                        .build())
                                                  .build())
                                  .build();
    }

}

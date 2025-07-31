package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.v3.LastOperation;
import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceResource;
import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceType;
import org.cloudfoundry.client.v3.serviceofferings.BrokerCatalog;
import org.cloudfoundry.client.v3.serviceofferings.Features;
import org.cloudfoundry.client.v3.serviceofferings.ServiceOfferingRelationships;
import org.cloudfoundry.client.v3.serviceofferings.ServiceOfferingResource;
import org.junit.jupiter.api.Test;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceOperation;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceOperation.State;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ServiceOperation.Type;

class RawCloudServiceTest {

    private static final String NAME = "my-db";
    private static final String OFFERING_NAME = "postgresql";
    private static final String PLAN_NAME = "v9.4-small";
    private static final List<String> TAGS = Arrays.asList("test-tag-1", "test-tag-2");
    private static final String LAST_OPERATION_TYPE_CREATE = "create";
    private static final String LAST_OPERATION_STATE_IN_PROGRESS = "in progress";
    private static final String LAST_OPERATION_STATE_SUCCEEDED = "succeeded";

    @Test
    void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedService(), buildRawService());
    }

    @Test
    void testDeriveWithUserProvidedService() {
        RawCloudEntityTest.testDerive(buildExpectedUserProvidedService(), buildRawUserProvidedService());
    }

    private static CloudServiceInstance buildExpectedService() {
        return ImmutableCloudServiceInstance.builder()
                                            .metadata(RawCloudEntityTest.EXPECTED_METADATA_PARSED_FROM_V3_RESOURCE)
                                            .name(NAME)
                                            .plan(PLAN_NAME)
                                            .label(OFFERING_NAME)
                                            .type(ServiceInstanceType.MANAGED)
                                            .tags(TAGS)
                                            .lastOperation(new ServiceOperation(Type.CREATE, "", State.IN_PROGRESS))
                                            .build();
    }

    private static CloudServiceInstance buildExpectedUserProvidedService() {
        return ImmutableCloudServiceInstance.builder()
                                            .metadata(RawCloudEntityTest.EXPECTED_METADATA_PARSED_FROM_V3_RESOURCE)
                                            .name(NAME)
                                            .type(ServiceInstanceType.USER_PROVIDED)
                                            .tags(TAGS)
                                            .lastOperation(new ServiceOperation(Type.CREATE, "", State.SUCCEEDED))
                                            .build();
    }

    private static RawCloudServiceInstance buildRawService() {
        return ImmutableRawCloudServiceInstance.builder()
                                               .resource(buildTestResource(false, LastOperation.builder()
                                                                                               .type(LAST_OPERATION_TYPE_CREATE)
                                                                                               .state(LAST_OPERATION_STATE_IN_PROGRESS)
                                                                                               .build()))
                                               .servicePlan(RawCloudServicePlanTest.buildTestServicePlan(PLAN_NAME))
                                               .serviceOffering(buildTestServiceOffering())
                                               .build();
    }

    private static RawCloudServiceInstance buildRawUserProvidedService() {
        return ImmutableRawCloudServiceInstance.builder()
                                               .resource(buildTestResource(true, LastOperation.builder()
                                                                                              .type(LAST_OPERATION_TYPE_CREATE)
                                                                                              .state(LAST_OPERATION_STATE_SUCCEEDED)
                                                                                              .build()))
                                               .build();
    }

    private static ServiceInstanceResource buildTestResource(boolean isUserProvided, LastOperation lastOperation) {
        ServiceInstanceResource.Builder serviceInstanceResourceBuilder = ServiceInstanceResource.builder()
                                                                                                .id(RawCloudEntityTest.GUID_STRING)
                                                                                                .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                                                                                                .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                                                                                                .name(NAME)
                                                                                                .type(ServiceInstanceType.MANAGED)
                                                                                                .addAllTags(TAGS)
                                                                                                .lastOperation(lastOperation);
        if (isUserProvided) {
            serviceInstanceResourceBuilder.type(ServiceInstanceType.USER_PROVIDED);
        }
        return serviceInstanceResourceBuilder.build();
    }

    private static ServiceOfferingResource buildTestServiceOffering() {
        return ServiceOfferingResource.builder()
                                      .id(RawCloudEntityTest.GUID_STRING)
                                      .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                                      .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                                      .available(RawCloudServiceOfferingTest.AVAILABLE)
                                      .name(OFFERING_NAME)
                                      .brokerCatalog(BrokerCatalog.builder()
                                                                  .brokerCatalogId(RawCloudServiceOfferingTest.UNIQUE_ID)
                                                                  .features(Features.builder()
                                                                                    .bindable(RawCloudServiceOfferingTest.BINDABLE)
                                                                                    .allowContextUpdates(RawCloudServiceOfferingTest.ALLOW_CONTEXT_UPDATES)
                                                                                    .bindingsRetrievable(RawCloudServiceOfferingTest.BINDINGS_RETRIEVABLE)
                                                                                    .instancesRetrievable(RawCloudServiceOfferingTest.INSTANCES_RETRIEVABLE)
                                                                                    .planUpdateable(RawCloudServiceOfferingTest.PLAN_UPDATEABLE)
                                                                                    .build())
                                                                  .build())
                                      .relationships(ServiceOfferingRelationships.builder()
                                                                                 .serviceBroker(ToOneRelationship.builder()
                                                                                                                 .data(Relationship.builder()
                                                                                                                                   .id(RawCloudServiceOfferingTest.SERVICE_BROKER_GUID)
                                                                                                                                   .build())
                                                                                                                 .build())
                                                                                 .build())
                                      .shareable(RawCloudServiceOfferingTest.SHAREABLE)
                                      .description(RawCloudServiceOfferingTest.DESCRIPTION)
                                      .documentationUrl(RawCloudServiceOfferingTest.DOCUMENTATION_URL)
                                      .build();
    }

}

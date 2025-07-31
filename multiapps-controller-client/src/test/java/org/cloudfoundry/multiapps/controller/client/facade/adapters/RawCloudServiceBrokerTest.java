package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.servicebrokers.ServiceBrokerRelationships;
import org.cloudfoundry.client.v3.servicebrokers.ServiceBrokerResource;
import org.junit.jupiter.api.Test;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBroker;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudServiceBroker;

class RawCloudServiceBrokerTest {

    private static final String NAME = "auditlog-broker";
    private static final String URL = "https://auditlog-broker.example.com";
    private static final String SPACE_GUID_STRING = "ef93547f-74c3-4bad-ba69-b7dc4f212622";

    @Test
    void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedServiceBroker(), buildRawServiceBroker());
    }

    private static CloudServiceBroker buildExpectedServiceBroker() {
        return ImmutableCloudServiceBroker.builder()
                                          .metadata(RawCloudEntityTest.EXPECTED_METADATA_PARSED_FROM_V3_RESOURCE)
                                          .name(NAME)
                                          .spaceGuid(SPACE_GUID_STRING)
                                          .url(URL)
                                          .build();
    }

    private static RawCloudServiceBroker buildRawServiceBroker() {
        return ImmutableRawCloudServiceBroker.builder()
                                             .serviceBroker(buildTestServiceBroker())
                                             .build();
    }

    private static ServiceBrokerResource buildTestServiceBroker() {
        return ServiceBrokerResource.builder()
                                    .id(RawCloudEntityTest.GUID_STRING)
                                    .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                                    .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                                    .name(NAME)
                                    .url(URL)
                                    .relationships(ServiceBrokerRelationships.builder()
                                                                             .space(ToOneRelationship.builder()
                                                                                                     .data(Relationship.builder()
                                                                                                                       .id(SPACE_GUID_STRING)
                                                                                                                       .build())
                                                                                                     .build())
                                                                             .build())
                                    .build();
    }

}

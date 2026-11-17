package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceBroker;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceBroker.V3RelationshipData;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceBroker.V3Relationships;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceBroker.V3ToOneRelationship;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class V3ServiceBrokerMapperTest {

    private static final String GUID_STRING = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";
    private static final String SPACE_GUID = "11111111-2222-3333-4444-555555555555";

    @Test
    void testToCloudServiceBrokerMapsAllFields() {
        V3Relationships relationships = new V3Relationships(new V3ToOneRelationship(new V3RelationshipData(SPACE_GUID)));
        V3ServiceBroker broker = new V3ServiceBroker(GUID_STRING, "my-broker", "https://broker", null, null, null, relationships);

        CloudServiceBroker result = V3ServiceBrokerMapper.toCloudServiceBroker(broker);

        Assertions.assertEquals("my-broker", result.getName());
        Assertions.assertEquals("https://broker", result.getUrl());
        Assertions.assertEquals(SPACE_GUID, result.getSpaceGuid());
    }

    @Test
    void testToCloudServiceBrokerNullRelationshipsReturnNullSpaceGuid() {
        V3ServiceBroker broker = new V3ServiceBroker(GUID_STRING, "my-broker", "https://broker", null, null, null, null);

        Assertions.assertNull(V3ServiceBrokerMapper.toCloudServiceBroker(broker)
                                                   .getSpaceGuid());
    }

    @Test
    void testToCloudServiceBrokerNullSpaceDataReturnsNullSpaceGuid() {
        V3Relationships relationships = new V3Relationships(new V3ToOneRelationship(null));
        V3ServiceBroker broker = new V3ServiceBroker(GUID_STRING, "my-broker", "https://broker", null, null, null, relationships);

        Assertions.assertNull(V3ServiceBrokerMapper.toCloudServiceBroker(broker)
                                                   .getSpaceGuid());
    }

}

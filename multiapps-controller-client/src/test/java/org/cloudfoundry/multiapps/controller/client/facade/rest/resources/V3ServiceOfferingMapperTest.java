package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServiceOffering;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceOffering.V3BrokerCatalog;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceOffering.V3Features;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceOffering.V3RelationshipData;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceOffering.V3Relationships;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServiceOffering.V3ToOneRelationship;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class V3ServiceOfferingMapperTest {

    private static final String GUID_STRING = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";
    private static final String BROKER_GUID = "11111111-2222-3333-4444-555555555555";

    @Test
    void testToCloudServiceOfferingMapsAllFields() {
        V3Features features = new V3Features(true, null, null, null, null);
        V3BrokerCatalog brokerCatalog = new V3BrokerCatalog("catalog-id", Map.of("displayName", "My Service"), features);
        V3Relationships relationships = new V3Relationships(new V3ToOneRelationship(new V3RelationshipData(BROKER_GUID)));
        V3ServiceOffering offering = new V3ServiceOffering(GUID_STRING, "my-offering", "An offering", true, true, "https://docs",
                                                           null, null, brokerCatalog, relationships, null);

        CloudServiceOffering result = V3ServiceOfferingMapper.toCloudServiceOffering(offering, List.of());

        Assertions.assertEquals("my-offering", result.getName());
        Assertions.assertEquals("An offering", result.getDescription());
        Assertions.assertTrue(result.isAvailable());
        Assertions.assertTrue(result.isBindable());
        Assertions.assertTrue(result.isShareable());
        Assertions.assertEquals("https://docs", result.getDocUrl());
        Assertions.assertEquals("catalog-id", result.getUniqueId());
        Assertions.assertEquals(BROKER_GUID, result.getBrokerId());
        Assertions.assertEquals(Map.of("displayName", "My Service"), result.getExtra());
    }

    @Test
    void testToCloudServiceOfferingNullBrokerCatalogReturnsNullDerivedFields() {
        V3ServiceOffering offering = new V3ServiceOffering(GUID_STRING, "o", null, true, false, null, null, null, null, null, null);

        CloudServiceOffering result = V3ServiceOfferingMapper.toCloudServiceOffering(offering, List.of());

        Assertions.assertNull(result.isBindable());
        Assertions.assertNull(result.getUniqueId());
        Assertions.assertNull(result.getExtra());
        Assertions.assertNull(result.getBrokerId());
    }

    @Test
    void testToCloudServiceOfferingNullFeaturesReturnsNullBindable() {
        V3BrokerCatalog brokerCatalog = new V3BrokerCatalog("catalog-id", null, null);
        V3ServiceOffering offering = new V3ServiceOffering(GUID_STRING, "o", null, true, false, null, null, null, brokerCatalog, null,
                                                           null);

        Assertions.assertNull(V3ServiceOfferingMapper.toCloudServiceOffering(offering, List.of())
                                                     .isBindable());
    }

    @Test
    void testToCloudServiceOfferingNullServiceBrokerRelationshipReturnsNullBrokerId() {
        V3Relationships relationships = new V3Relationships(null);
        V3ServiceOffering offering = new V3ServiceOffering(GUID_STRING, "o", null, true, false, null, null, null, null, relationships,
                                                           null);

        Assertions.assertNull(V3ServiceOfferingMapper.toCloudServiceOffering(offering, List.of())
                                                     .getBrokerId());
    }

}

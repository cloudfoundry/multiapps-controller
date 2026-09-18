package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudServicePlan;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServicePlan.V3BrokerCatalog;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServicePlan.V3RelationshipData;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServicePlan.V3Relationships;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ServicePlan.V3ToOneRelationship;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class V3ServicePlanMapperTest {

    private static final String GUID_STRING = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";
    private static final String OFFERING_GUID = "11111111-2222-3333-4444-555555555555";

    @Test
    void testToCloudServicePlanMapsAllFields() {
        V3BrokerCatalog brokerCatalog = new V3BrokerCatalog("catalog-id", Map.of("k", "v"));
        V3Relationships relationships = new V3Relationships(new V3ToOneRelationship(new V3RelationshipData(OFFERING_GUID)));
        V3ServicePlan plan = new V3ServicePlan(GUID_STRING, "my-plan", "A plan", true, "public", null, null, brokerCatalog,
                                               relationships, null);

        CloudServicePlan result = V3ServicePlanMapper.toCloudServicePlan(plan);

        Assertions.assertEquals("my-plan", result.getName());
        Assertions.assertEquals("A plan", result.getDescription());
        Assertions.assertTrue(result.isFree());
        Assertions.assertTrue(result.isPublic());
        Assertions.assertEquals("catalog-id", result.getUniqueId());
        Assertions.assertEquals(OFFERING_GUID, result.getServiceOfferingId());
        Assertions.assertEquals(Map.of("k", "v"), result.getExtra());
    }

    @Test
    void testToCloudServicePlanNonPublicVisibilityIsNotPublic() {
        V3ServicePlan plan = buildPlanWithVisibility("organization");

        Assertions.assertFalse(V3ServicePlanMapper.toCloudServicePlan(plan)
                                                  .isPublic());
    }

    @Test
    void testToCloudServicePlanNullVisibilityReturnsNullIsPublic() {
        V3ServicePlan plan = buildPlanWithVisibility(null);

        Assertions.assertNull(V3ServicePlanMapper.toCloudServicePlan(plan)
                                                 .isPublic());
    }

    @Test
    void testToCloudServicePlanNullBrokerCatalogAndRelationshipsReturnNulls() {
        V3ServicePlan plan = new V3ServicePlan(GUID_STRING, "p", null, false, "public", null, null, null, null, null);

        CloudServicePlan result = V3ServicePlanMapper.toCloudServicePlan(plan);

        Assertions.assertNull(result.getUniqueId());
        Assertions.assertNull(result.getExtra());
        Assertions.assertNull(result.getServiceOfferingId());
    }

    private static V3ServicePlan buildPlanWithVisibility(String visibilityType) {
        return new V3ServicePlan(GUID_STRING, "p", null, false, visibilityType, null, null, null, null, null);
    }

}

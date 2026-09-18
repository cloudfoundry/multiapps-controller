package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Domain.V3DomainRelationships;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Domain.V3RelationshipData;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Domain.V3ToOneRelationship;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class V3DomainMapperTest {

    private static final String GUID_STRING = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";
    private static final String ORG_GUID = "11111111-2222-3333-4444-555555555555";

    @Test
    void testToCloudDomainMapsNameAndGuid() {
        V3Domain domain = new V3Domain(GUID_STRING, "example.com", null, null, null, null);

        CloudDomain result = V3DomainMapper.toCloudDomain(domain);

        Assertions.assertEquals("example.com", result.getName());
        Assertions.assertEquals(GUID_STRING, result.getGuid()
                                                   .toString());
    }

    @Test
    void testToCloudDomainWithNullGuidReturnsNullGuid() {
        V3Domain domain = new V3Domain(null, "example.com", null, null, null, null);

        Assertions.assertNull(V3DomainMapper.toCloudDomain(domain)
                                            .getMetadata()
                                            .getGuid());
    }

    @Test
    void testToCloudDomainIsPrivateWhenOrganizationDataPresent() {
        V3DomainRelationships relationships = new V3DomainRelationships(new V3ToOneRelationship(new V3RelationshipData(ORG_GUID)));
        V3Domain domain = new V3Domain(GUID_STRING, "private.example.com", null, null, null, relationships);

        Assertions.assertTrue(domain.isPrivate());
    }

    @Test
    void testToCloudDomainIsNotPrivateWhenRelationshipsNull() {
        V3Domain domain = new V3Domain(GUID_STRING, "shared.example.com", null, null, null, null);

        Assertions.assertFalse(domain.isPrivate());
    }

    @Test
    void testToCloudDomainIsNotPrivateWhenOrganizationDataNull() {
        V3DomainRelationships relationships = new V3DomainRelationships(new V3ToOneRelationship(null));
        V3Domain domain = new V3Domain(GUID_STRING, "shared.example.com", null, null, null, relationships);

        Assertions.assertFalse(domain.isPrivate());
    }

}

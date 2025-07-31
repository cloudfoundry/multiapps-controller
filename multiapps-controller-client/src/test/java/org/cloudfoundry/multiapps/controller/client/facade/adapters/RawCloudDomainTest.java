package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import org.cloudfoundry.client.v3.ToManyRelationship;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.domains.Domain;
import org.cloudfoundry.client.v3.domains.DomainRelationships;
import org.cloudfoundry.client.v3.organizations.GetOrganizationDefaultDomainResponse;
import org.junit.jupiter.api.Test;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudDomain;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudDomain;

class RawCloudDomainTest {

    private static final String DOMAIN_NAME = "example.com";

    @Test
    void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedDomain(), buildRawDomain());
    }

    private static CloudDomain buildExpectedDomain() {
        return ImmutableCloudDomain.builder()
                                   .metadata(RawCloudEntityTest.EXPECTED_METADATA_PARSED_FROM_V3_RESOURCE)
                                   .name(DOMAIN_NAME)
                                   .build();
    }

    private static RawCloudDomain buildRawDomain() {
        return ImmutableRawCloudDomain.of(buildTestResource());
    }

    private static Domain buildTestResource() {
        return GetOrganizationDefaultDomainResponse.builder()
                                                   .id(RawCloudEntityTest.GUID_STRING)
                                                   .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                                                   .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                                                   .name(DOMAIN_NAME)
                                                   .isInternal(false)
                                                   .relationships(DomainRelationships.builder()
                                                                                     .organization(ToOneRelationship.builder()
                                                                                                                    .build())
                                                                                     .sharedOrganizations(ToManyRelationship.builder()
                                                                                                                            .build())
                                                                                     .build())
                                                   .build();
    }

}

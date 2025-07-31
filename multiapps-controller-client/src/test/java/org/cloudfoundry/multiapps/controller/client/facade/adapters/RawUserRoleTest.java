package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.roles.RoleRelationships;
import org.cloudfoundry.client.v3.roles.RoleResource;
import org.cloudfoundry.client.v3.roles.RoleType;
import org.junit.jupiter.api.Test;

import org.cloudfoundry.multiapps.controller.client.facade.domain.UserRole;

class RawUserRoleTest {

    private static final String USER_ROLE_GUID = "c94b4588-3431-11ed-a261-0242ac120002";
    private static final String USER_GUID = "328eaf4e-3432-11ed-a261-0242ac120002";
    private static final String DATE = "2022-01-01T13:35:11Z";
    private static final RoleType USER_ROLE_TYPE = RoleType.SPACE_DEVELOPER;

    @Test
    void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedUserRole(), buildActualUserRole());
    }

    private UserRole buildExpectedUserRole() {
        return UserRole.SPACE_DEVELOPER;
    }

    private RawUserRole buildActualUserRole() {
        return ImmutableRawUserRole.builder()
                                   .roleResource(buildRoleResource())
                                   .build();
    }

    private RoleResource buildRoleResource() {
        return RoleResource.builder()
                           .createdAt(DATE)
                           .id(USER_ROLE_GUID)
                           .type(USER_ROLE_TYPE)
                           .relationships(RoleRelationships.builder()
                                                           .user(ToOneRelationship.builder()
                                                                                  .data(Relationship.builder()
                                                                                                    .id(USER_GUID)
                                                                                                    .build())
                                                                                  .build())
                                                           .build())
                           .build();
    }

}

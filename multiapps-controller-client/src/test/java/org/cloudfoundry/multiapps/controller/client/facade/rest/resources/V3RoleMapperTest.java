package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.UserRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class V3RoleMapperTest {

    @ParameterizedTest
    @CsvSource({ "organization_auditor,ORGANIZATION_AUDITOR", "organization_billing_manager,ORGANIZATION_BILLING_MANAGER",
                 "organization_manager,ORGANIZATION_MANAGER", "organization_user,ORGANIZATION_USER", "space_auditor,SPACE_AUDITOR",
                 "space_developer,SPACE_DEVELOPER", "space_manager,SPACE_MANAGER" })
    void testToUserRoleMapsEachType(String wireType, UserRole expected) {
        V3Role role = new V3Role("guid", null, null, wireType);

        Assertions.assertEquals(expected, V3RoleMapper.toUserRole(role));
    }

    @Test
    void testToUserRoleWithUnknownTypeThrows() {
        V3Role role = new V3Role("guid", null, null, "galactic_overlord");

        Assertions.assertThrows(IllegalArgumentException.class, () -> V3RoleMapper.toUserRole(role));
    }

}

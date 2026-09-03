package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.UserRole;

public final class V3RoleMapper {

    private V3RoleMapper() {
    }

    public static UserRole toUserRole(V3Role role) {
        return UserRole.fromRoleType(role.type());
    }

}

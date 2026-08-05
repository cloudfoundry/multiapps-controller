package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.UserRole;

/**
 * Maps the {@link V3Role} wire model to the project's {@link UserRole} domain enum. Passes the raw CF v3 role {@code type} wire value
 * (e.g. {@code space_developer}) straight to {@link UserRole#fromRoleType(String)}, so this mapper depends on no OSS types.
 */
public final class V3RoleMapper {

    private V3RoleMapper() {
    }

    public static UserRole toUserRole(V3Role role) {
        return UserRole.fromRoleType(role.type());
    }

}

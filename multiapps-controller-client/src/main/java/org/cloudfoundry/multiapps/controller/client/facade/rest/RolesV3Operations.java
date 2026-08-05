package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.UserRole;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Role;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3RoleMapper;
import org.springframework.core.ParameterizedTypeReference;

/**
 * CF v3 {@code Roles} operations for the in-house {@link CloudControllerRestClient} replacement. Implements the role-related methods of
 * the interface by listing {@code GET /v3/roles} filtered by space and user, and mapping each wire {@link V3Role} to the project's
 * {@link UserRole} domain enum via {@link V3RoleMapper}.
 *
 * <p>
 * Mirrors the OSS {@code CloudControllerRestClientImpl#getUserRolesBySpaceAndUser}, which lists roles filtered by {@code space_id},
 * {@code user_id} and all role {@code types}, then collects the results into an {@link EnumSet}.
 * </p>
 */
public class RolesV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3Role>> ROLE_PAGE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client cc;
    private final CloudSpace target;

    public RolesV3Operations(CloudControllerV3Client cc, CloudSpace target) {
        this.cc = cc;
        this.target = target;
    }

    public Set<UserRole> getUserRolesBySpaceAndUser(UUID spaceGuid, UUID userGuid) {
        String uri = "/v3/roles?per_page=5000&space_guids=" + spaceGuid + "&user_guids=" + userGuid;
        return cc.list(uri, ROLE_PAGE)
                 .stream()
                 .map(V3RoleMapper::toUserRole)
                 .collect(Collectors.toCollection(() -> EnumSet.noneOf(UserRole.class)));
    }

}

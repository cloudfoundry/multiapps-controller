package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.client.facade.domain.UserRole;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3QueryParameters;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Role;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3RoleMapper;
import org.springframework.core.ParameterizedTypeReference;

public class RolesV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3Role>> ROLE_PAGE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client client;

    public RolesV3Operations(CloudControllerV3Client client) {
        this.client = client;
    }

    public Set<UserRole> getUserRolesBySpaceAndUser(UUID spaceGuid, UUID userGuid) {
        String uri = V3Uri.path(CloudControllerV3Endpoints.ROLES)
                          .perPage(CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE)
                          .query(V3QueryParameters.SPACE_GUIDS, spaceGuid)
                          .query(V3QueryParameters.USER_GUIDS, userGuid)
                          .build();

        return client.list(uri, ROLE_PAGE)
                     .stream()
                     .map(V3RoleMapper::toUserRole)
                     .collect(Collectors.toCollection(() -> EnumSet.noneOf(UserRole.class)));
    }

}

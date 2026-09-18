package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.client.facade.domain.UserRole;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Role;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3RoleMapper;
import org.springframework.core.ParameterizedTypeReference;

public class RolesV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3Role>> ROLE_PAGE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client cc;

    public RolesV3Operations(CloudControllerV3Client cc) {
        this.cc = cc;
    }

    public Set<UserRole> getUserRolesBySpaceAndUser(UUID spaceGuid, UUID userGuid) {
        String uri = CloudControllerV3Endpoints.ROLES + CloudControllerV3Endpoints.QUERY_PER_PAGE
            + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE + CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS + spaceGuid
            + CloudControllerV3Endpoints.AMPERSAND_USER_GUIDS + userGuid;

        return cc.list(uri, ROLE_PAGE)
                 .stream()
                 .map(V3RoleMapper::toUserRole)
                 .collect(Collectors.toCollection(() -> EnumSet.noneOf(UserRole.class)));
    }

}

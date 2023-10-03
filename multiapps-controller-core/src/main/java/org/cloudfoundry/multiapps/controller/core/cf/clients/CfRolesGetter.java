package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;

import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.domain.UserRole;

public class CfRolesGetter extends CustomControllerClient {

    private static final String ROLES_FILTER = Arrays.stream(UserRole.values())
                                                     .map(UserRole::getName)
                                                     .collect(Collectors.joining(","));
    private static final String GET_ROLES_URL = "/v3/roles?space_guids=%s&user_guids=%s&types=" + ROLES_FILTER;

    public CfRolesGetter(ApplicationConfiguration configuration, WebClientFactory webClientFactory, CloudCredentials credentials) {
        super(configuration, webClientFactory, credentials);
    }

    public Set<UserRole> getRoles(UUID spaceGuid, UUID userGuid) {
        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(() -> doGetRoles(spaceGuid, userGuid));
    }

    private Set<UserRole> doGetRoles(UUID spaceGuid, UUID userGuid) {
        String url = String.format(GET_ROLES_URL, spaceGuid, userGuid);
        var list = getListOfResources(new UserRoleMapper(), url);
        var result = EnumSet.noneOf(UserRole.class);
        result.addAll(list);
        return result;
    }

    private static class UserRoleMapper extends ResourcesResponseMapper<UserRole> {

        @Override
        public List<UserRole> getMappedResources() {
            return getQueriedResources().stream()
                                        .map(this::mapToUserRole)
                                        .collect(Collectors.toList());
        }

        private UserRole mapToUserRole(Map<String, Object> role) {
            return UserRole.valueOf(((String) role.get("type")).toUpperCase());
        }
    }

}

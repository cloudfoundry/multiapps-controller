package org.cloudfoundry.multiapps.controller.web.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingFacade;
import org.cloudfoundry.multiapps.controller.core.auditlogging.AuditLoggingProvider;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudOrganization;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudOrganization;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudSpace;
import com.sap.cloudfoundry.client.facade.domain.ImmutableUserRole;
import com.sap.cloudfoundry.client.facade.domain.UserRole;

class AuthorizationCheckerTest {

    private static final String ORG = "org";
    private static final String SPACE = "space";
    private static final String USERNAME = "userName";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SPACE_ID = UUID.randomUUID();

    @Mock
    private CloudControllerClient client;
    @Mock
    private CloudControllerClientProvider clientProvider;
    @Mock
    private ApplicationConfiguration applicationConfiguration;
    @InjectMocks
    private AuthorizationChecker authorizationChecker;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    static Stream<Arguments> checkPermissionsUsingNamesTest() {
        return Stream.of(
                         // (0) User has a space developer role and has access
                         Arguments.of(List.of(UserRole.SpaceRole.SPACE_DEVELOPER), true),
                         // (1) User does not have any roles and has access
                         Arguments.of(Collections.emptyList(), false),
                         // (2) User does not have any roles and no access
                         Arguments.of(Collections.emptyList(), false));
    }

    static Stream<Arguments> checkPermissionUsingGuidsTest() {
        return Stream.of(
                         // (0) User has a space developer role and executes a non read-only request
                         Arguments.of(List.of(UserRole.SpaceRole.SPACE_DEVELOPER), false, true),
                         // (1) User does not have any roles and executes a non read-only request
                         Arguments.of(Collections.emptyList(), false, false),
                         // (2) User does not have any roles and executes a read-only request
                         Arguments.of(Collections.emptyList(), true, false),
                         // (3) User has a space auditor role and executes a non read-only request
                         Arguments.of(List.of(UserRole.SpaceRole.SPACE_AUDITOR), false, false),
                         // (4) User has a space manager role and executes a non read-only request
                         Arguments.of(List.of(UserRole.SpaceRole.SPACE_MANAGER), false, false),
                         // (5) User has a space auditor role and executes a read-only request
                         Arguments.of(List.of(UserRole.SpaceRole.SPACE_AUDITOR), true, true),
                         // (6) User has a space manager role and executes a read-only request
                         Arguments.of(List.of(UserRole.SpaceRole.SPACE_MANAGER), true, true),
                         // (7) User has a space developer role and executes a read-only request
                         Arguments.of(List.of(UserRole.SpaceRole.SPACE_DEVELOPER), true, true));
    }

    @ParameterizedTest
    @MethodSource
    void checkPermissionsUsingNamesTest(List<UserRole.SpaceRole> spaceRoles, boolean shouldBeAuthorized) {
        setUpMocks(spaceRoles, null);
        mockSpace();
        boolean isAuthorized = authorizationChecker.checkPermissions(getUserInfo(), ORG, SPACE, false);
        assertEquals(shouldBeAuthorized, isAuthorized);
    }

    @Test
    void checkPermissionsWithExceptionTest() {
        setUpMocks(List.of(UserRole.SpaceRole.SPACE_DEVELOPER), new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        assertThrows(Exception.class, () -> authorizationChecker.checkPermissions(getUserInfo(), ORG, SPACE, false));
    }

    @ParameterizedTest
    @MethodSource
    void checkPermissionUsingGuidsTest(List<UserRole.SpaceRole> spaceRoles, boolean isReadOnly, boolean shouldBeAuthorized) {
        setUpMocks(spaceRoles, null);
        boolean isAuthorized = authorizationChecker.checkPermissions(getUserInfo(), SPACE_ID.toString(), isReadOnly);
        assertEquals(shouldBeAuthorized, isAuthorized);
    }

    @Test
    void checkPermissionsWithExceptionTest2() {
        setUpMocks(List.of(UserRole.SpaceRole.SPACE_DEVELOPER), new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        assertThrows(Exception.class, () -> authorizationChecker.checkPermissions(getUserInfo(), SPACE_ID.toString(), false));
    }

    @Test
    void testCheckPermissionsWithNonUUIDSpaceIDString() {
        setUpMocks(List.of(UserRole.SpaceRole.SPACE_DEVELOPER), null);
        AuditLoggingFacade mockAuditLoggingFacade = Mockito.mock(AuditLoggingFacade.class);
        AuditLoggingProvider.setFacade(mockAuditLoggingFacade);
        UserInfo userInfo = getUserInfo();
        ResponseStatusException resultException = assertThrows(ResponseStatusException.class,
                                                               () -> authorizationChecker.checkPermissions(userInfo, "non-uuid-spaceId",
                                                                                                           true));
        assertEquals(HttpStatus.NOT_FOUND, resultException.getStatus());
    }

    private void setUpMocks(List<UserRole.SpaceRole> spaceRoles, Exception exception) {
        UserRole userRole = getUserRole(spaceRoles);
        when(client.getUserRoleBySpaceGuidAndUserGuid(SPACE_ID, USER_ID)).thenReturn(userRole);
        setUpException(exception);
        when(clientProvider.getControllerClient(getUserInfo().getName())).thenReturn(client);
        when(applicationConfiguration.getFssCacheUpdateTimeoutMinutes()).thenReturn(ApplicationConfiguration.DEFAULT_SPACE_DEVELOPER_CACHE_TIME_IN_SECONDS);
    }

    private void setUpException(Exception exception) {
        if (exception != null) {
            when(client.getSpace(ORG, SPACE)).thenThrow(exception);
            when(client.getUserRoleBySpaceGuidAndUserGuid(SPACE_ID, USER_ID)).thenThrow(exception);
        }
    }

    private void mockSpace() {
        CloudOrganization organization = getOrganization();
        when(client.getSpace(ORG, SPACE)).thenReturn(getCloudSpace(organization));
    }

    private ImmutableCloudOrganization getOrganization() {
        return ImmutableCloudOrganization.builder()
                                         .name(ORG)
                                         .build();
    }

    private CloudSpace getCloudSpace(CloudOrganization organization) {
        return ImmutableCloudSpace.builder()
                                  .metadata(ImmutableCloudMetadata.builder()
                                                                  .guid(SPACE_ID)
                                                                  .build())
                                  .name(SPACE)
                                  .organization(organization)
                                  .build();
    }

    private UserRole getUserRole(List<UserRole.SpaceRole> spaceRoles) {
        return ImmutableUserRole.builder()
                                .spaceRoles(spaceRoles)
                                .build();
    }

    private UserInfo getUserInfo() {
        DefaultOAuth2AccessToken accessToken = new DefaultOAuth2AccessToken("testTokenValue");
        accessToken.setScope(new HashSet<>());
        return new UserInfo(USER_ID.toString(), USERNAME, accessToken);
    }

}

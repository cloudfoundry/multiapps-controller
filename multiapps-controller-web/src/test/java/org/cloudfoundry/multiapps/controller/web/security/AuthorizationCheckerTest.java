package org.cloudfoundry.multiapps.controller.web.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.EnumSet;
import java.util.UUID;
import java.util.Set;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientFactory;
import org.cloudfoundry.multiapps.controller.core.cf.clients.CfRolesGetter;
import org.cloudfoundry.multiapps.controller.core.cf.clients.WebClientFactory;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.core.util.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloudfoundry.client.facade.domain.CloudOrganization;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudOrganization;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudSpace;
import com.sap.cloudfoundry.client.facade.domain.UserRole;
import com.sap.cloudfoundry.client.facade.rest.CloudSpaceClient;
import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

class AuthorizationCheckerTest {

    private static final String ORG = "org";
    private static final String SPACE = "space";
    private static final String USERNAME = "userName";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SPACE_ID = UUID.randomUUID();

    @Mock
    private CloudControllerClientFactory clientFactory;
    @Mock
    private TokenService tokenService;
    @Mock
    private WebClientFactory webClientFactory;
    @Mock
    private CfRolesGetter rolesGetter;
    private final ApplicationConfiguration applicationConfiguration = new ApplicationConfiguration();
    private AuthorizationChecker authorizationChecker;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        authorizationChecker = new AuthorizationChecker(clientFactory, tokenService, applicationConfiguration, webClientFactory) {
            @Override
            protected CfRolesGetter getRolesGetter(OAuth2AccessTokenWithAdditionalInfo token) {
                return rolesGetter;
            }
        };
    }

    static Stream<Arguments> checkPermissionsUsingNamesTest() {
        return Stream.of(
                         // (0) User has a space developer role and has access
                         Arguments.of(EnumSet.of(UserRole.SPACE_DEVELOPER), true),
                         // (1) User has org user & manager roles and no access
                         Arguments.of(EnumSet.of(UserRole.ORGANIZATION_USER, UserRole.ORGANIZATION_MANAGER), false),
                         // (2) User has a space manager & developer roles and has access
                         Arguments.of(EnumSet.of(UserRole.SPACE_MANAGER, UserRole.SPACE_DEVELOPER), true),
                         // (3) User does not have any roles and no access
                         Arguments.of(Collections.emptySet(), false));
    }

    static Stream<Arguments> checkPermissionUsingGuidsTest() {
        return Stream.of(
                         // (0) User has a space developer role and executes a non read-only request
                         Arguments.of(EnumSet.of(UserRole.SPACE_DEVELOPER), false, true),
                         // (1) User does not have any roles and executes a non read-only request
                         Arguments.of(Collections.emptySet(), false, false),
                         // (2) User does not have any roles and executes a read-only request
                         Arguments.of(Collections.emptySet(), true, false),
                         // (3) User has a space auditor role and executes a non read-only request
                         Arguments.of(EnumSet.of(UserRole.SPACE_AUDITOR), false, false),
                         // (4) User has a space manager role and executes a non read-only request
                         Arguments.of(EnumSet.of(UserRole.SPACE_MANAGER), false, false),
                         // (5) User has a space auditor role and executes a read-only request
                         Arguments.of(EnumSet.of(UserRole.SPACE_AUDITOR), true, true),
                         // (6) User has a space manager role and executes a read-only request
                         Arguments.of(EnumSet.of(UserRole.SPACE_MANAGER), true, true),
                         // (7) User has a space developer role and executes a read-only request
                         Arguments.of(EnumSet.of(UserRole.SPACE_DEVELOPER), true, true),
                         // (8) User has a space auditor & manager roles and executes a read-only request
                         Arguments.of(EnumSet.of(UserRole.SPACE_AUDITOR, UserRole.SPACE_MANAGER), true, true),
                         // (9) User has a org user & manager roles and executes a read-only request
                         Arguments.of(EnumSet.of(UserRole.ORGANIZATION_USER, UserRole.ORGANIZATION_MANAGER), true, false),
                         // (10) User has a org user & manager roles and executes a read-only request
                         Arguments.of(EnumSet.of(UserRole.ORGANIZATION_USER, UserRole.ORGANIZATION_MANAGER), false, false));
    }

    @ParameterizedTest
    @MethodSource
    void checkPermissionsUsingNamesTest(Set<UserRole> spaceRoles, boolean shouldBeAuthorized) {
        setUpMocks(spaceRoles, null);
        mockSpace();
        boolean isAuthorized = authorizationChecker.checkPermissions(getUserInfo(), ORG, SPACE, false);
        assertEquals(shouldBeAuthorized, isAuthorized);
    }

    @Test
    void checkPermissionsWithExceptionTest() {
        setUpMocks(EnumSet.of(UserRole.SPACE_DEVELOPER), new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        assertThrows(Exception.class, () -> authorizationChecker.checkPermissions(getUserInfo(), ORG, SPACE, false));
    }

    @ParameterizedTest
    @MethodSource
    void checkPermissionUsingGuidsTest(Set<UserRole> spaceRoles, boolean isReadOnly, boolean shouldBeAuthorized) {
        setUpMocks(spaceRoles, null);
        boolean isAuthorized = authorizationChecker.checkPermissions(getUserInfo(), SPACE_ID.toString(), isReadOnly);
        assertEquals(shouldBeAuthorized, isAuthorized);
    }

    @Test
    void checkPermissionsWithExceptionTest2() {
        setUpMocks(EnumSet.of(UserRole.SPACE_DEVELOPER), new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        assertThrows(Exception.class, () -> authorizationChecker.checkPermissions(getUserInfo(), SPACE_ID.toString(), false));
    }

    @Test
    void testCheckPermissionsWithNonUUIDSpaceIDString() {
        setUpMocks(EnumSet.of(UserRole.SPACE_DEVELOPER), null);
        UserInfo userInfo = getUserInfo();
        ResponseStatusException resultException = assertThrows(ResponseStatusException.class,
                                                               () -> authorizationChecker.checkPermissions(userInfo, "non-uuid-spaceId",
                                                                                                           true));
        assertEquals(HttpStatus.NOT_FOUND, resultException.getStatusCode());
    }

    private void setUpMocks(Set<UserRole> spaceRoles, Exception exception) {
        var token = Mockito.mock(OAuth2AccessTokenWithAdditionalInfo.class);
        when(tokenService.getToken(anyString())).thenReturn(token);
        if (exception != null) {
            when(rolesGetter.getRoles(SPACE_ID, USER_ID)).thenThrow(exception);
        } else {
            when(rolesGetter.getRoles(SPACE_ID, USER_ID)).thenReturn(spaceRoles);
        }
    }

    private void mockSpace() {
        CloudOrganization organization = getOrganization();

        var spaceClient = Mockito.mock(CloudSpaceClient.class);
        when(spaceClient.getSpace(anyString(), anyString())).thenReturn(getCloudSpace(organization));
        when(clientFactory.createSpaceClient(any())).thenReturn(spaceClient);
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

    private UserInfo getUserInfo() {
        OAuth2AccessTokenWithAdditionalInfo accessToken = new OAuth2AccessTokenWithAdditionalInfo(new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                                                                                                                        "token_value",
                                                                                                                        Instant.now(),
                                                                                                                        Instant.now().plus(5, ChronoUnit.MINUTES)),
                                                                                                  Collections.emptyMap());
        return new UserInfo(USER_ID.toString(), USERNAME, accessToken);
    }

}

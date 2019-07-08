package com.sap.cloud.lm.sl.cf.web.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import com.sap.cloud.lm.sl.cf.core.model.CloudTarget;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.ImmutableCloudOrganization;
import org.cloudfoundry.client.lib.domain.ImmutableCloudSpace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.web.client.HttpClientErrorException;

import com.sap.cloud.lm.sl.cf.core.cf.CloudControllerClientProvider;
import com.sap.cloud.lm.sl.cf.core.helpers.ClientHelper;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;

public class AuthorizationCheckerTest {

    private UserInfo userInfo;
    private static final String ORG = "org";
    private static final String SPACE = "space";
    private static final UUID USER_ID = UUID.fromString("8e44f008-9b07-3e18-a718-eb9ca3d94674");
    private static final String USERNAME = "userName";
    private static final String SPACE_ID = "a72df2e8-b06c-44b2-a8fa-5cadb0239573";
    private static final String SECOND_SPACE_ID = "a72df2e8-b06c-44b2-a8fa-00001234abcd";
    private static final String THIRD_SPACE_ID = "1234f2e8-b06c-44b2-a8fa-000012344321";

    @Mock
    private CloudControllerClientProvider clientProvider;
    @Mock
    private CloudControllerClient client;
    @Mock
    private ApplicationConfiguration applicationConfiguration;
    @Spy
    @InjectMocks
    private AuthorizationChecker authorizationChecker;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    public static Stream<Arguments> checkPermissionsTest() {
        return Stream.of(
            // (0) User has access
            Arguments.of(true, true),
            // (1) User has access but no permissions
            Arguments.of(true, false),
            // (2) User has permissions but no access
            Arguments.of(false, true),
            // (3) User has no permissions and no access
            Arguments.of(false, false));
    }

    public static Stream<Arguments> checkPermissionTest2() {
        return Stream.of(
            // (0) User has access
            Arguments.of(true, true),
            // (1) User has access but no permissions
            // Arguments.of(true, false),
            // (3) User has permissions but no access
            Arguments.of(false, true));
        // (4) User has no permissions and no access
        // Arguments.of(false, false));
    }

    @ParameterizedTest
    @MethodSource
    public void checkPermissionsTest(boolean hasPermissions, boolean hasAccess) {
        setUpMocks(hasPermissions, hasAccess, null);

        boolean isAuthorized = authorizationChecker.checkPermissions(userInfo, ORG, SPACE, false);
        boolean shouldBeAuthorized = hasAccess && hasPermissions;
        assertEquals(shouldBeAuthorized, isAuthorized);
    }

    @Test
    public void checkPermissionsWithExceptionTest() {
        setUpMocks(true, true, new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        assertThrows(Exception.class, () -> authorizationChecker.checkPermissions(userInfo, ORG, SPACE, false));
    }

    @ParameterizedTest
    @MethodSource
    public void checkPermissionTest2(boolean hasPermissions, boolean hasAccess) {
        setUpMocks(hasPermissions, hasAccess, null);
        boolean isAuthorized = authorizationChecker.checkPermissions(userInfo, SPACE_ID, false);
        boolean shouldBeAuthorized = hasAccess && hasPermissions;
        assertEquals(shouldBeAuthorized, isAuthorized);
    }

    @Test
    public void checkPermissionsWithExceptionTest2() {
        setUpMocks(true, true, new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        assertThrows(Exception.class, () -> authorizationChecker.checkPermissions(userInfo, SPACE_ID, false));
    }

    @Test
    public void testSpaceDevelopersCache() {
        setUpMocks(true, true, null);
        when(client.getSpaceDevelopers(UUID.fromString(SPACE_ID))).thenReturn(Arrays.asList(USER_ID));
        when(client.getSpaceDevelopers(UUID.fromString(SECOND_SPACE_ID))).thenReturn(Arrays.asList(USER_ID));

        assertTrue(authorizationChecker.checkPermissions(userInfo, SPACE_ID, false));
        Mockito.verify(client, Mockito.times(1))
            .getSpaceDevelopers(Mockito.eq(UUID.fromString(SPACE_ID)));

        assertTrue(authorizationChecker.checkPermissions(userInfo, SPACE_ID, false));
        Mockito.verify(client, Mockito.times(1))
            .getSpaceDevelopers(Mockito.eq(UUID.fromString(SPACE_ID)));

        assertTrue(authorizationChecker.checkPermissions(userInfo, SECOND_SPACE_ID, false));
        Mockito.verify(client, Mockito.times(1))
            .getSpaceDevelopers(Mockito.eq(UUID.fromString(SPACE_ID)));
        Mockito.verify(client, Mockito.times(1))
            .getSpaceDevelopers(Mockito.eq(UUID.fromString(SECOND_SPACE_ID)));
    }

    @Test
    public void testSpaceDevelopersCacheNegativeResult() {
        setUpMocks(true, true, null);
        when(client.getSpaceDevelopers(Mockito.eq(UUID.fromString(THIRD_SPACE_ID)))).thenReturn(Arrays.asList(USER_ID));

        assertTrue(authorizationChecker.checkPermissions(userInfo, THIRD_SPACE_ID, false));
        Mockito.verify(client, Mockito.times(1))
            .getSpaceDevelopers(Mockito.eq(UUID.fromString(THIRD_SPACE_ID)));

        UUID newUserId = UUID.fromString("6c02b5bc-b9b1-38d7-b332-1dfdb2ba85a0");
        UserInfo negativeUser = new UserInfo(newUserId.toString(), "newUser", userInfo.getToken());
        when(client.getSpaceDevelopers(Mockito.eq(UUID.fromString(THIRD_SPACE_ID)))).thenReturn(Arrays.asList(USER_ID, newUserId));
        when(clientProvider.getControllerClient(negativeUser.getName())).thenReturn(client);

        assertTrue(authorizationChecker.checkPermissions(negativeUser, THIRD_SPACE_ID, false));
        Mockito.verify(client, Mockito.times(2))
            .getSpaceDevelopers(Mockito.eq(UUID.fromString(THIRD_SPACE_ID)));
    }

    private void setUpMocks(boolean hasPermissions, boolean hasAccess, Exception e) {
        DefaultOAuth2AccessToken accessToken = new DefaultOAuth2AccessToken("testTokenValue");
        accessToken.setScope(new HashSet<>());
        CloudOrganization organization = ImmutableCloudOrganization.builder()
            .name(ORG)
            .build();
        CloudSpace space = ImmutableCloudSpace.builder()
            .name(SPACE)
            .organization(organization)
            .build();
        ClientHelper clientHelper = Mockito.mock(ClientHelper.class);

        if (hasAccess) {
            when(client.getSpace(ORG, SPACE, false)).thenReturn(space);
            when(clientHelper.computeTarget(SPACE_ID)).thenReturn(new CloudTarget(ORG, SPACE));
        } else {
            when(clientHelper.computeTarget(SPACE_ID)).thenReturn(null);
        }
        when(authorizationChecker.getClientHelper(client)).thenReturn(clientHelper);
        userInfo = new UserInfo(USER_ID.toString(), USERNAME, accessToken);
        List<UUID> spaceDevelopersList = new ArrayList<>();
        if (hasPermissions) {
            spaceDevelopersList.add(USER_ID);
        }

        if (e == null) {
            when(client.getSpaceDevelopers(ORG, SPACE)).thenReturn(spaceDevelopersList);
            when(client.getSpaceDevelopers(UUID.fromString(SPACE_ID))).thenReturn(spaceDevelopersList);
        } else {
            when(client.getSpaceDevelopers(ORG, SPACE)).thenThrow(e);
            when(client.getSpaceDevelopers(UUID.fromString(SPACE_ID))).thenThrow(e);
        }

        when(clientProvider.getControllerClient(userInfo.getName())).thenReturn(client);
        when(applicationConfiguration.getFssCacheUpdateTimeoutMinutes())
            .thenReturn(ApplicationConfiguration.DEFAULT_SPACE_DEVELOPER_CACHE_TIME_IN_SECONDS);
    }
}

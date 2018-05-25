package com.sap.cloud.lm.sl.cf.web.security;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
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

import com.sap.cloud.lm.sl.cf.client.CloudFoundryOperationsExtended;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.clients.SpaceGetter;
import com.sap.cloud.lm.sl.cf.core.helpers.ClientHelper;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;
import com.sap.cloud.lm.sl.common.util.Pair;

public class AuthorizationCheckerTest {

    private UserInfo userInfo;
    private static final String ORG = "org";
    private static final String SPACE = "space";
    private static final String USER_ID = "userId";
    private static final String USERNAME = "userName";
    private static final String SPACE_ID = "spaceID";

    @Mock
    private CloudFoundryClientProvider clientProvider;
    @Mock
    private CloudFoundryOperationsExtended client;
    @Mock
    private SpaceGetter spaceGetter;
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
        setUpMocks(true, false, null);
        assertThrows(Exception.class, () -> authorizationChecker.checkPermissions(userInfo, SPACE_ID, false));

        setUpMocks(false, false, null);
        assertThrows(Exception.class, () -> authorizationChecker.checkPermissions(userInfo, SPACE_ID, false));

        setUpMocks(true, true, new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        assertThrows(Exception.class, () -> authorizationChecker.checkPermissions(userInfo, SPACE_ID, false));
    }

    private void setUpMocks(boolean hasPermissions, boolean hasAccess, Exception e) {
        DefaultOAuth2AccessToken accessToken = new DefaultOAuth2AccessToken("testTokenValue");
        accessToken.setScope(new HashSet<>());
        CloudSpace space = new CloudSpace(null, SPACE, new CloudOrganization(null, ORG));
        ClientHelper clientHelper = Mockito.mock(ClientHelper.class);

        if (hasAccess) {
            when(spaceGetter.findSpace(client, ORG, SPACE)).thenReturn(space);
            when(clientHelper.computeOrgAndSpace(SPACE_ID)).thenReturn(new Pair<String, String>(ORG, SPACE));
        } else {
            when(clientHelper.computeOrgAndSpace(SPACE_ID)).thenReturn(null);
        }
        when(authorizationChecker.getClientHelper(client)).thenReturn(clientHelper);
        userInfo = new UserInfo(USER_ID, USERNAME, accessToken);
        List<String> spaceDevelopersList = new ArrayList<>();
        if (hasPermissions) {
            spaceDevelopersList.add(USER_ID);
        }

        if (e == null) {
            when(client.getSpaceDevelopers2(ORG, SPACE)).thenReturn(spaceDevelopersList);
        } else {
            when(client.getSpaceDevelopers2(ORG, SPACE)).thenThrow(e);
        }

        when(clientProvider.getCloudFoundryClient(userInfo.getName())).thenReturn(client);
    }
}

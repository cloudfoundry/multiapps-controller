package com.sap.cloud.lm.sl.cf.web.security;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.web.client.HttpClientErrorException;

import com.sap.cloud.lm.sl.cf.client.CloudFoundryOperationsExtended;
import com.sap.cloud.lm.sl.cf.core.cf.CloudFoundryClientProvider;
import com.sap.cloud.lm.sl.cf.core.cf.clients.SpaceGetter;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.cf.core.util.UserInfo;

@RunWith(Parameterized.class)
public class AuthorizationCheckerTest {

    private UserInfo userInfo;
    private static final String ORG = "org";
    private static final String SPACE = "space";
    private static final String USER_ID = "userId";
    private static final String USERNAME = "userName";
    private boolean hasPermissions;
    private boolean hasAccess;
    private Exception exception;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private CloudFoundryClientProvider clientProvider;
    @Mock
    private CloudFoundryOperationsExtended client;
    @Mock
    private SpaceGetter spaceGetter;
    @Mock
    private ApplicationConfiguration applicationConfiguration;
    @InjectMocks
    private AuthorizationChecker authorizationChecker;

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) User has access
            {
                true, true, null
            },
            // (1) User has access but no permissions
            {
                true, false, null
            },
            // (2) Fail with 400 Bad Request:
            {
                true, true, new HttpClientErrorException(HttpStatus.BAD_REQUEST)
            },
            // (3) User has permissions but no access
            {
                false, true, null
            },
            // (4) User has no permissions and no access
            {
                false, false, null
            }
// @formatter:on
        });
    }

    public AuthorizationCheckerTest(boolean hasAccess, boolean hasPermissions, Exception exception) {
        this.hasAccess = hasAccess;
        this.hasPermissions = hasPermissions;
        this.exception = exception;
    }

    @Before
    public void setUp() {
        setUpMocks();
        setUpException();
    }

    private void setUpMocks() {
        MockitoAnnotations.initMocks(this);
        DefaultOAuth2AccessToken accessToken = new DefaultOAuth2AccessToken("testTokenValue");
        accessToken.setScope(new HashSet<>());
        CloudSpace space = new CloudSpace(null, SPACE, new CloudOrganization(null, ORG));
        if (hasAccess) {
            when(spaceGetter.findSpace(client, ORG, SPACE)).thenReturn(space);
        }

        userInfo = new UserInfo(USER_ID, USERNAME, accessToken);
        List<String> spaceDevelopersList = new ArrayList<>();
        if (hasPermissions) {
            spaceDevelopersList.add(USER_ID);
        }
        when(client.getSpaceDevelopers2(ORG, SPACE)).thenReturn(spaceDevelopersList);
        when(clientProvider.getCloudFoundryClient(userInfo.getName())).thenReturn(client);
    }

    private void setUpException() {
        if (exception != null) {
            expectedException.expect(exception.getClass());
            when(client.getSpaceDevelopers2(ORG, SPACE)).thenThrow(exception);
        }
    }

    @Test
    public void checkPermissionsTest() {
        boolean isAuthorized = authorizationChecker.checkPermissions(userInfo, ORG, SPACE, false);
        boolean shouldBeAuthorized = hasAccess && hasPermissions;
        assertTrue(shouldBeAuthorized == isAuthorized);
    }
}

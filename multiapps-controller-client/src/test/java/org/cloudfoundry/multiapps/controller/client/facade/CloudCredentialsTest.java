package org.cloudfoundry.multiapps.controller.client.facade;

import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CloudCredentialsTest {

    @Mock
    private OAuth2AccessTokenWithAdditionalInfo token;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
    }

    static Stream<Arguments> emailPasswordConstructors() {
        return Stream.of(Arguments.of(new CloudCredentials("alice@example.com", "secret"),
                                      "alice@example.com",
                                      "secret",
                                      "cf",
                                      "",
                                      null),
                         Arguments.of(new CloudCredentials("a", "b", "client-x"), "a", "b", "client-x", "", null),
                         Arguments.of(new CloudCredentials("a", "b", "cid", "csecret"), "a", "b", "cid", "csecret", null),
                         Arguments.of(new CloudCredentials("a", "b", "cid", "csecret", "uaa"), "a", "b", "cid", "csecret", "uaa"));
    }

    @ParameterizedTest
    @MethodSource("emailPasswordConstructors")
    void testEmailPasswordConstructorsAssignAllFields(CloudCredentials credentials, String email, String password, String clientId,
                                                     String clientSecret, String origin) {
        Assertions.assertEquals(email, credentials.getEmail());
        Assertions.assertEquals(password, credentials.getPassword());
        Assertions.assertEquals(clientId, credentials.getClientId());
        Assertions.assertEquals(clientSecret, credentials.getClientSecret());
        Assertions.assertEquals(origin, credentials.getOrigin());
        Assertions.assertNull(credentials.getToken());
        Assertions.assertNull(credentials.getProxyUser());
        Assertions.assertFalse(credentials.isProxyUserSet());
        Assertions.assertTrue(credentials.isRefreshable());
    }

    @Test
    void testTokenAndRefreshableConstructorRespectsRefreshableFlag() {
        CloudCredentials refreshable = new CloudCredentials(token);
        CloudCredentials nonRefreshable = new CloudCredentials(token, false);

        Assertions.assertSame(token, refreshable.getToken());
        Assertions.assertTrue(refreshable.isRefreshable());
        Assertions.assertSame(token, nonRefreshable.getToken());
        Assertions.assertFalse(nonRefreshable.isRefreshable());
    }

    @Test
    void testTokenWithClientCredentialsConstructorsAssignClientId() {
        CloudCredentials withClientId = new CloudCredentials(token, "client-y");
        CloudCredentials withClientIdAndSecret = new CloudCredentials(token, "client-y", "shh");

        Assertions.assertEquals("client-y", withClientId.getClientId());
        Assertions.assertEquals("client-y", withClientIdAndSecret.getClientId());
        Assertions.assertEquals("shh", withClientIdAndSecret.getClientSecret());
    }

    @Test
    void testProxyForUserCopiesEmailPasswordClientIdAndTokenAndSetsProxyUser() {
        CloudCredentials base = new CloudCredentials("a@b.c", "pw", "cid");

        CloudCredentials proxied = base.proxyForUser("admin");

        Assertions.assertEquals("a@b.c", proxied.getEmail());
        Assertions.assertEquals("pw", proxied.getPassword());
        Assertions.assertEquals("cid", proxied.getClientId());
        Assertions.assertEquals("admin", proxied.getProxyUser());
        Assertions.assertTrue(proxied.isProxyUserSet());
    }

    @Test
    void testProxyForUserWithTokenCopiesToken() {
        CloudCredentials base = new CloudCredentials(token);

        CloudCredentials proxied = base.proxyForUser("admin");

        Assertions.assertSame(token, proxied.getToken());
        Assertions.assertEquals("admin", proxied.getProxyUser());
    }
}

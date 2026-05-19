package org.cloudfoundry.multiapps.controller.client.facade;

import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    @Test
    void testEmailPasswordConstructorDefaultsClientIdAndClientSecret() {
        CloudCredentials c = new CloudCredentials("alice@example.com", "secret");

        Assertions.assertEquals("alice@example.com", c.getEmail());
        Assertions.assertEquals("secret", c.getPassword());
        Assertions.assertEquals("cf", c.getClientId());
        Assertions.assertEquals("", c.getClientSecret());
        Assertions.assertNull(c.getOrigin());
        Assertions.assertNull(c.getProxyUser());
        Assertions.assertFalse(c.isProxyUserSet());
        Assertions.assertTrue(c.isRefreshable());
        Assertions.assertNull(c.getToken());
    }

    @Test
    void testEmailPasswordClientIdConstructor() {
        CloudCredentials c = new CloudCredentials("a", "b", "client-x");

        Assertions.assertEquals("client-x", c.getClientId());
    }

    @Test
    void testEmailPasswordClientIdSecretConstructor() {
        CloudCredentials c = new CloudCredentials("a", "b", "cid", "csecret");

        Assertions.assertEquals("cid", c.getClientId());
        Assertions.assertEquals("csecret", c.getClientSecret());
    }

    @Test
    void testEmailPasswordClientIdSecretOriginConstructor() {
        CloudCredentials c = new CloudCredentials("a", "b", "cid", "csecret", "uaa");

        Assertions.assertEquals("uaa", c.getOrigin());
    }

    @Test
    void testTokenOnlyConstructor() {
        CloudCredentials c = new CloudCredentials(token);

        Assertions.assertSame(token, c.getToken());
        Assertions.assertTrue(c.isRefreshable());
    }

    @Test
    void testTokenAndRefreshableConstructor() {
        CloudCredentials c = new CloudCredentials(token, false);

        Assertions.assertSame(token, c.getToken());
        Assertions.assertFalse(c.isRefreshable());
    }

    @Test
    void testTokenAndClientIdConstructor() {
        CloudCredentials c = new CloudCredentials(token, "client-y");

        Assertions.assertEquals("client-y", c.getClientId());
    }

    @Test
    void testTokenClientIdSecretConstructor() {
        CloudCredentials c = new CloudCredentials(token, "client-y", "shh");

        Assertions.assertEquals("client-y", c.getClientId());
        Assertions.assertEquals("shh", c.getClientSecret());
    }

    @Test
    void testProxyForUserCopiesFieldsAndSetsProxyUser() {
        CloudCredentials base = new CloudCredentials("a@b.c", "pw", "cid");

        CloudCredentials proxied = base.proxyForUser("admin");

        Assertions.assertEquals("a@b.c", proxied.getEmail());
        Assertions.assertEquals("pw", proxied.getPassword());
        Assertions.assertEquals("cid", proxied.getClientId());
        Assertions.assertEquals("admin", proxied.getProxyUser());
        Assertions.assertTrue(proxied.isProxyUserSet());
    }
}

package org.cloudfoundry.multiapps.controller.web.upload.client.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.cloudfoundry.multiapps.controller.api.model.ImmutableUserCredentials;
import org.cloudfoundry.multiapps.controller.api.model.UserCredentials;
import org.junit.jupiter.api.Test;

class AuthenticatorResolverTest {

    private static final String PLAIN_URL = "https://example.com/file.mtar";
    private static final String URL_WITH_USERINFO = "https://user:pass@example.com/file.mtar";
    private static final String TOKEN = "ghp_token";
    private static final String USERNAME = "user";
    private static final String PASSWORD = "pass";

    private final AuthenticatorResolver resolver = new AuthenticatorResolver();

    @Test
    void tokenResolvesToBearer() {
        UserCredentials credentials = ImmutableUserCredentials.builder()
                                                              .token(TOKEN)
                                                              .build();
        RequestAuthenticator authenticator = resolver.resolve(credentials, PLAIN_URL);
        assertInstanceOf(BearerTokenAuthenticator.class, authenticator);
    }

    @Test
    void tokenTakesPrecedenceOverUsernamePassword() {
        UserCredentials credentials = ImmutableUserCredentials.builder()
                                                              .username(USERNAME)
                                                              .password(PASSWORD)
                                                              .token(TOKEN)
                                                              .build();
        RequestAuthenticator authenticator = resolver.resolve(credentials, PLAIN_URL);
        assertInstanceOf(BearerTokenAuthenticator.class, authenticator);
    }

    @Test
    void usernamePasswordResolvesToBasic() {
        UserCredentials credentials = ImmutableUserCredentials.builder()
                                                              .username(USERNAME)
                                                              .password(PASSWORD)
                                                              .build();
        RequestAuthenticator authenticator = resolver.resolve(credentials, PLAIN_URL);
        assertInstanceOf(BasicAuthenticator.class, authenticator);
    }

    @Test
    void urlUserInfoResolvesToUrlUserInfoAuthenticator() {
        RequestAuthenticator authenticator = resolver.resolve(null, URL_WITH_USERINFO);
        assertInstanceOf(UrlUserInfoAuthenticator.class, authenticator);
    }

    @Test
    void noCredentialsResolvesToNoAuthenticator() {
        RequestAuthenticator authenticator = resolver.resolve(null, PLAIN_URL);
        assertInstanceOf(NoAuthenticator.class, authenticator);
    }

    @Test
    void bearerAuthenticatorSetsBearerHeader() {
        HttpRequest request = applyToRequest(new BearerTokenAuthenticator(TOKEN), URI.create(PLAIN_URL));
        assertEquals("Bearer " + TOKEN, authorizationHeader(request));
        assertEquals(PLAIN_URL, request.uri()
                                       .toString());
    }

    @Test
    void basicAuthenticatorSetsBasicHeader() {
        HttpRequest request = applyToRequest(new BasicAuthenticator(USERNAME, PASSWORD), URI.create(PLAIN_URL));
        String expected = "Basic " + Base64.getEncoder()
                                           .encodeToString("user:pass".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, authorizationHeader(request));
    }

    @Test
    void urlUserInfoAuthenticatorStripsUserInfoAndSetsBasicHeader() {
        URI uri = URI.create(URL_WITH_USERINFO);
        HttpRequest request = applyToRequest(new UrlUserInfoAuthenticator(URL_WITH_USERINFO), uri);
        assertFalse(request.uri()
                           .toString()
                           .contains("user:pass@"));
        String expected = "Basic " + Base64.getEncoder()
                                           .encodeToString("user:pass".getBytes(StandardCharsets.UTF_8));
        assertEquals(expected, authorizationHeader(request));
    }

    @Test
    void noAuthenticatorSetsNoHeader() {
        HttpRequest request = applyToRequest(new NoAuthenticator(), URI.create(PLAIN_URL));
        assertTrue(request.headers()
                          .firstValue("Authorization")
                          .isEmpty());
        assertEquals(PLAIN_URL, request.uri()
                                       .toString());
    }

    private HttpRequest applyToRequest(RequestAuthenticator authenticator, URI uri) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                                                 .GET();
        authenticator.applyTo(builder, uri);
        return builder.build();
    }

    private String authorizationHeader(HttpRequest request) {
        return request.headers()
                      .firstValue("Authorization")
                      .orElseThrow();
    }
}

package org.cloudfoundry.multiapps.controller.web.upload.client.auth;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.http.HttpHeaders;

/**
 * HTTP Basic authenticator built from an explicit username and password. Encodes {@code username:password}
 * as Base64 and sets the {@code Authorization: Basic ...} header. The URI is used as-is.
 */
public class BasicAuthenticator implements RequestAuthenticator {

    private final String username;
    private final String password;

    public BasicAuthenticator(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void applyTo(HttpRequest.Builder builder, URI uri) {
        builder.uri(uri);
        String credentials = username + ":" + password;
        String encodedAuth = Base64.getEncoder()
                                   .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        builder.header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
    }
}

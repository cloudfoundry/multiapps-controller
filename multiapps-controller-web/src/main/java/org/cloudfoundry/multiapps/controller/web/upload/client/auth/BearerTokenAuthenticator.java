package org.cloudfoundry.multiapps.controller.web.upload.client.auth;

import java.net.URI;
import java.net.http.HttpRequest;

import org.springframework.http.HttpHeaders;

/**
 * Bearer-token authenticator. Sets {@code Authorization: Bearer <token>} with the caller-supplied token
 * verbatim and targets the URI as-is. This is the scheme required by GitHub (fine-grained/classic PATs,
 * GitHub App installation tokens, {@code GITHUB_TOKEN}), which all present identically on the wire.
 */
public class BearerTokenAuthenticator implements RequestAuthenticator {

    private final String token;

    public BearerTokenAuthenticator(String token) {
        this.token = token;
    }

    @Override
    public void applyTo(HttpRequest.Builder builder, URI uri) {
        builder.uri(uri);
        builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }
}

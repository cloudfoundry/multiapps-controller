package org.cloudfoundry.multiapps.controller.web.upload.client.auth;

import java.net.URI;
import java.net.http.HttpRequest;

/**
 * No-op authenticator: targets the URI as-is and attaches no {@code Authorization} header. Used when
 * the request carries no credentials.
 */
public class NoAuthenticator implements RequestAuthenticator {

    @Override
    public void applyTo(HttpRequest.Builder builder, URI uri) {
        builder.uri(uri);
    }
}

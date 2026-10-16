package org.cloudfoundry.multiapps.controller.web.upload.client.auth;

import java.net.URI;
import java.net.http.HttpRequest;

/**
 * Strategy for attaching authentication to an outbound deploy-from-URL fetch request.
 * <p>
 * Each implementation owns a single authentication scheme: how it encodes its credential and which
 * header (if any) it sets. The downloader stays agnostic of Base64 encoding and header names.
 * <p>
 * The resolver ({@link AuthenticatorResolver}) picks exactly one authenticator per request based on
 * the supplied credentials and the URL. Implementations must not mutate anything other than the
 * request builder (and the request URI, when they strip embedded user info from it).
 */
public interface RequestAuthenticator {

    /**
     * Applies this scheme to the request being built.
     *
     * @param builder the request builder to mutate (set the URI and, when applicable, the {@code Authorization} header)
     * @param uri     the target URI as parsed from the decoded URL
     */
    void applyTo(HttpRequest.Builder builder, URI uri);
}

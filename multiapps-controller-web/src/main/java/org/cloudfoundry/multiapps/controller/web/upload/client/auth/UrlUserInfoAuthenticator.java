package org.cloudfoundry.multiapps.controller.web.upload.client.auth;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.http.HttpHeaders;

/**
 * HTTP Basic authenticator built from user info embedded in the URL itself ({@code https://user:pass@host/...}).
 * Strips the raw user-info component from the URI (so it is not sent in the request line) and sets
 * {@code Authorization: Basic ...} from the decoded user-info string.
 * <p>
 * This preserves the legacy behavior for callers that embed credentials in the URL; it is deliberately
 * not extended for new schemes — new credentials should be passed as explicit fields instead.
 */
public class UrlUserInfoAuthenticator implements RequestAuthenticator {

    private final String decodedUrl;

    public UrlUserInfoAuthenticator(String decodedUrl) {
        this.decodedUrl = decodedUrl;
    }

    @Override
    public void applyTo(HttpRequest.Builder builder, URI uri) {
        builder.uri(URI.create(decodedUrl.replace(uri.getRawUserInfo() + "@", "")));
        String encodedAuth = Base64.getEncoder()
                                   .encodeToString(uri.getUserInfo()
                                                      .getBytes(StandardCharsets.UTF_8));
        builder.header(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
    }
}

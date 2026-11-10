package org.cloudfoundry.multiapps.controller.web.upload.client.auth;

import java.net.URI;

import jakarta.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.multiapps.controller.api.model.UserCredentials;

/**
 * Resolves the single {@link RequestAuthenticator} to apply to a deploy-from-URL fetch, from the
 * request's credentials and the target URI. The precedence is stated here, in one place:
 * <ol>
 * <li>{@code token} on the credentials object &rarr; {@link BearerTokenAuthenticator}</li>
 * <li>username/password on the credentials object &rarr; {@link BasicAuthenticator}</li>
 * <li>user info embedded in the URL ({@code user:pass@host}) &rarr; {@link UrlUserInfoAuthenticator}</li>
 * <li>otherwise &rarr; {@link NoAuthenticator}</li>
 * </ol>
 * Adding a new scheme means adding one branch here plus its authenticator class — the downloader and
 * the job/orchestration code stay untouched.
 */
@Named
public class AuthenticatorResolver {

    public RequestAuthenticator resolve(UserCredentials userCredentials, String decodedUrl) {
        if (userCredentials != null) {
            if (StringUtils.isNotBlank(userCredentials.getToken())) {
                return new BearerTokenAuthenticator(userCredentials.getToken());
            }
            return new BasicAuthenticator(userCredentials.getUsername(), userCredentials.getPassword());
        }
        if (URI.create(decodedUrl).getUserInfo() != null) {
            return new UrlUserInfoAuthenticator(decodedUrl);
        }
        return new NoAuthenticator();
    }
}

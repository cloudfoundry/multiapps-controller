package org.cloudfoundry.multiapps.controller.client.facade;

import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

/**
 * Class that encapsulates credentials used for authentication
 *
 */
public class CloudCredentials {

    private String clientId = "cf";
    private String clientSecret = "";
    private String email;
    private String password;
    private String proxyUser;
    private String origin;
    private boolean refreshable = true;
    private OAuth2AccessTokenWithAdditionalInfo token;

    /**
     * Create credentials using email and password.
     *
     * @param email email to authenticate with
     * @param password the password
     */
    public CloudCredentials(String email, String password) {
        this.email = email;
        this.password = password;
    }

    /**
     * Create credentials using email, password, and client ID.
     *
     * @param email email to authenticate with
     * @param password the password
     * @param clientId the client ID to use for authorization
     */
    public CloudCredentials(String email, String password, String clientId) {
        this.email = email;
        this.password = password;
        this.clientId = clientId;
    }

    /**
     * Create credentials using email, password and client ID.
     *
     * @param email email to authenticate with
     * @param password the password
     * @param clientId the client ID to use for authorization
     * @param clientSecret the secret for the given client
     */
    public CloudCredentials(String email, String password, String clientId, String clientSecret) {
        this.email = email;
        this.password = password;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * Create credentials using email, password, client ID and login origin.
     *
     * @param email email to authenticate with
     * @param password the password
     * @param clientId the client ID to use for authorization
     * @param clientSecret the secret for the given client
     * @param origin the origin name
     */
    public CloudCredentials(String email, String password, String clientId, String clientSecret, String origin) {
        this.email = email;
        this.password = password;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.origin = origin;
    }

    /**
     * Create credentials using a token.
     *
     * @param token token to use for authorization
     */
    public CloudCredentials(OAuth2AccessTokenWithAdditionalInfo token) {
        this.token = token;
    }

    /**
     * Create credentials using a token and indicates if the token is refreshable or not.
     *
     * @param token token to use for authorization
     * @param refreshable indicates if the token can be refreshed or not
     */
    public CloudCredentials(OAuth2AccessTokenWithAdditionalInfo token, boolean refreshable) {
        this.token = token;
        this.refreshable = refreshable;
    }

    /**
     * Create credentials using a token.
     *
     * @param token token to use for authorization
     * @param clientId the client ID to use for authorization
     */
    public CloudCredentials(OAuth2AccessTokenWithAdditionalInfo token, String clientId) {
        this.token = token;
        this.clientId = clientId;
    }

    /**
     * Create credentials using a token.
     *
     * @param token token to use for authorization
     * @param clientId the client ID to use for authorization
     * @param clientSecret the password for the specified client
     */
    public CloudCredentials(OAuth2AccessTokenWithAdditionalInfo token, String clientId, String clientSecret) {
        this.token = token;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * Create proxy credentials.
     *
     * @param cloudCredentials credentials to use
     * @param proxyForUser user to be proxied
     */
    public CloudCredentials(CloudCredentials cloudCredentials, String proxyForUser) {
        this.email = cloudCredentials.getEmail();
        this.password = cloudCredentials.getPassword();
        this.clientId = cloudCredentials.getClientId();
        this.token = cloudCredentials.getToken();
        this.proxyUser = proxyForUser;
    }

    /**
     * Get the client ID.
     *
     * @return the client ID
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Get the client secret
     *
     * @return the client secret
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * Get the origin
     *
     * @return the origin
     */
    public String getOrigin() {
        return origin;
    }

    /**
     * Get the email.
     *
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Get the password.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Get the proxy user.
     *
     * @return the proxy user
     */
    public String getProxyUser() {
        return proxyUser;
    }

    /**
     * Get the token.
     *
     * @return the token
     */
    public OAuth2AccessTokenWithAdditionalInfo getToken() {
        return token;
    }

    /**
     * Is this a proxied set of credentials?
     *
     * @return whether a proxy user is set
     */
    public boolean isProxyUserSet() {
        return proxyUser != null;
    }

    /**
     * Indicates weather the token stored in the cloud credentials can be refreshed or not. This is useful when the token stored in this
     * object was obtained via implicit OAuth2 authentication and therefore can not be refreshed.
     *
     * @return weather the token can be refreshed
     */
    public boolean isRefreshable() {
        return refreshable;
    }

    /**
     * Run commands as a different user. The authenticated user must be privileged to run as this user.
     *
     * @param user the user to proxy for
     * @return credentials for the proxied user
     */
    public CloudCredentials proxyForUser(String user) {
        return new CloudCredentials(this, user);
    }
}

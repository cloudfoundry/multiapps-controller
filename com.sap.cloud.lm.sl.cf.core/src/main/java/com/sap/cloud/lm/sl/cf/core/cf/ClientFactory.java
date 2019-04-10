package com.sap.cloud.lm.sl.cf.core.cf;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import com.sap.cloud.lm.sl.cf.client.TokenProvider;
import com.sap.cloud.lm.sl.cf.core.util.SecurityUtil;
import com.sap.cloud.lm.sl.common.util.Pair;

public abstract class ClientFactory {

    public Pair<CloudControllerClient, TokenProvider> createClient(String userName, String password) {
        return createClient(createCredentials(userName, password));
    }

    public Pair<CloudControllerClient, TokenProvider> createClient(OAuth2AccessToken token) {
        return createClient(createCredentials(token));
    }

    public Pair<CloudControllerClient, TokenProvider> createClient(OAuth2AccessToken token, String org, String space) {
        return createClient(createCredentials(token), org, space);
    }

    public Pair<CloudControllerClient, TokenProvider> createClient(OAuth2AccessToken token, String spaceId) {
        return createClient(createCredentials(token), spaceId);
    }

    protected abstract Pair<CloudControllerClient, TokenProvider> createClient(CloudCredentials credentials);

    protected abstract Pair<CloudControllerClient, TokenProvider> createClient(CloudCredentials credentials, String org, String space);

    protected abstract Pair<CloudControllerClient, TokenProvider> createClient(CloudCredentials credentials, String spaceId);

    private static CloudCredentials createCredentials(String userName, String password) {
        return new CloudCredentials(userName, password, SecurityUtil.CLIENT_ID, SecurityUtil.CLIENT_SECRET);
    }

    private static CloudCredentials createCredentials(OAuth2AccessToken token) {
        boolean refreshable = (token.getRefreshToken() != null);
        return new CloudCredentials(token, refreshable);
    }
}

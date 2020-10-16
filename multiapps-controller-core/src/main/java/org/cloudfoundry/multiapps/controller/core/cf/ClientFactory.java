package org.cloudfoundry.multiapps.controller.core.cf;

import org.springframework.security.oauth2.common.OAuth2AccessToken;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudCredentials;

public abstract class ClientFactory {

    public CloudControllerClient createClient(OAuth2AccessToken token) {
        return createClient(createCredentials(token));
    }

    public CloudControllerClient createClient(OAuth2AccessToken token, String org, String space) {
        return createClient(createCredentials(token), org, space);
    }

    public CloudControllerClient createClient(OAuth2AccessToken token, String spaceId) {
        return createClient(createCredentials(token), spaceId);
    }

    protected abstract CloudControllerClient createClient(CloudCredentials credentials);

    protected abstract CloudControllerClient createClient(CloudCredentials credentials, String org, String space);

    protected abstract CloudControllerClient createClient(CloudCredentials credentials, String spaceId);

    private static CloudCredentials createCredentials(OAuth2AccessToken token) {
        boolean refreshable = (token.getRefreshToken() != null);
        return new CloudCredentials(token, refreshable);
    }
}

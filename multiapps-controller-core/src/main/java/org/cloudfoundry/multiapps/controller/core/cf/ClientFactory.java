package org.cloudfoundry.multiapps.controller.core.cf;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;

public abstract class ClientFactory {

    public CloudControllerClient createClient(OAuth2AccessTokenWithAdditionalInfo token) {
        return createClient(createCredentials(token));
    }

    public CloudControllerClient createClient(OAuth2AccessTokenWithAdditionalInfo token, String org, String space) {
        return createClient(createCredentials(token), org, space);
    }

    public CloudControllerClient createClient(OAuth2AccessTokenWithAdditionalInfo token, String spaceId) {
        return createClient(createCredentials(token), spaceId);
    }

    protected abstract CloudControllerClient createClient(CloudCredentials credentials);

    protected abstract CloudControllerClient createClient(CloudCredentials credentials, String org, String space);

    protected abstract CloudControllerClient createClient(CloudCredentials credentials, String spaceId);

    private static CloudCredentials createCredentials(OAuth2AccessTokenWithAdditionalInfo token) {
        return new CloudCredentials(token, true);
    }
}

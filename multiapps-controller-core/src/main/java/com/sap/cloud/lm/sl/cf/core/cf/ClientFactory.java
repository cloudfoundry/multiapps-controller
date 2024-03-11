package com.sap.cloud.lm.sl.cf.core.cf;

import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.springframework.beans.factory.annotation.Autowired;

import com.sap.cloud.lm.sl.cf.client.TokenProvider;
import com.sap.cloud.lm.sl.cf.client.uaa.UAAClient;
import com.sap.cloud.lm.sl.cf.core.cf.service.TokenService;
import com.sap.cloud.lm.sl.cf.core.util.SecurityUtil;
import com.sap.cloud.lm.sl.common.util.Pair;

@Named("cloudFoundryClientFactory")
public abstract class ClientFactory {

    @Autowired
    protected TokenService tokenService;

    @Autowired
    private UAAClient uaaClient;

    private static CloudCredentials createCredentials(String userName, String password) {
        return new CloudCredentials(userName, password, SecurityUtil.CF_CLIENT_ID, SecurityUtil.CLIENT_SECRET);
    }

    public Pair<CloudControllerClient, TokenProvider> createClient(String userName, String password) {
        return createClient(createCredentials(userName, password));
    }

    public Pair<CloudControllerClient, TokenProvider> createClient(OAuth2AccessTokenWithAdditionalInfo token) {
        return createClient(new CloudCredentials(token));
    }

    public Pair<CloudControllerClient, TokenProvider> createClient(OAuth2AccessTokenWithAdditionalInfo token, String org, String space) {
        return createClient(new CloudCredentials(token), org, space);
    }

    public Pair<CloudControllerClient, TokenProvider> createClient(OAuth2AccessTokenWithAdditionalInfo token, String spaceId) {
        return createClient(new CloudCredentials(token), spaceId);
    }

    protected abstract Pair<CloudControllerClient, TokenProvider> createClient(CloudCredentials credentials);

    protected abstract Pair<CloudControllerClient, TokenProvider> createClient(CloudCredentials credentials, String org, String space);

    protected abstract Pair<CloudControllerClient, TokenProvider> createClient(CloudCredentials credentials, String spaceId);

}

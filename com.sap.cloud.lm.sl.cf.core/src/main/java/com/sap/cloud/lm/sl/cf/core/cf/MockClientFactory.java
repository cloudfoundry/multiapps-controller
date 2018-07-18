package com.sap.cloud.lm.sl.cf.core.cf;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudControllerClient;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import com.sap.cloud.lm.sl.cf.client.TokenProvider;
import com.sap.cloud.lm.sl.common.util.Pair;

public class MockClientFactory extends ClientFactory {

    @Override
    protected Pair<CloudControllerClient, TokenProvider> createClient(CloudCredentials credentials) {
        return createClient();
    }

    @Override
    protected Pair<CloudControllerClient, TokenProvider> createClient(CloudCredentials credentials, String org, String space) {
        return createClient();
    }

    private static Pair<CloudControllerClient, TokenProvider> createClient() {
        return new Pair<>(new MockCloudFoundryClient(), createMockTokenProvider());
    }

    private static TokenProvider createMockTokenProvider() {
        return new TokenProvider() {
            @Override
            public OAuth2AccessToken getToken() {
                return null;
            }
        };
    }

    @Override
    protected Pair<CloudControllerClient, TokenProvider> createClient(CloudCredentials credentials, String spaceId) {
        return createClient();
    }

}

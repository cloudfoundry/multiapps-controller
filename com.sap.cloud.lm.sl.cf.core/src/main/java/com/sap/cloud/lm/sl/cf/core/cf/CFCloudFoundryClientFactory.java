package com.sap.cloud.lm.sl.cf.core.cf;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.rest.CloudControllerClient;
import org.cloudfoundry.client.lib.rest.CloudControllerClientFactory;

import com.sap.cloud.lm.sl.cf.client.CloudFoundryClientExtended;
import com.sap.cloud.lm.sl.cf.client.CloudFoundryTokenProvider;
import com.sap.cloud.lm.sl.cf.client.TokenProvider;
import com.sap.cloud.lm.sl.common.util.Pair;

public class CFCloudFoundryClientFactory extends CloudFoundryClientFactory {

    @Override
    protected Pair<CloudFoundryOperations, TokenProvider> createClient(CloudCredentials credentials) {
        CloudControllerClientFactory ccf = new CloudControllerClientFactory(null, false);
        CloudControllerClient cc = ccf.newCloudController(cloudControllerUrl, credentials, null);
        return new Pair<CloudFoundryOperations, TokenProvider>(new CloudFoundryClientExtended(cc),
            new CloudFoundryTokenProvider(ccf.getOauthClient()));
    }

    @Override
    protected Pair<CloudFoundryOperations, TokenProvider> createClient(CloudCredentials credentials, String org, String space) {
        CloudControllerClientFactory ccf = new CloudControllerClientFactory(null, false);
        CloudControllerClient cc = ccf.newCloudController(cloudControllerUrl, credentials, org, space);
        return new Pair<CloudFoundryOperations, TokenProvider>(new CloudFoundryClientExtended(cc),
            new CloudFoundryTokenProvider(ccf.getOauthClient()));
    }

}

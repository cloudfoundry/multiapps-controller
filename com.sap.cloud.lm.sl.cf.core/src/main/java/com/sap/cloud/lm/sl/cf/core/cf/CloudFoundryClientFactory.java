package com.sap.cloud.lm.sl.cf.core.cf;

import java.util.ArrayList;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.oauth2.OauthClient;
import org.cloudfoundry.client.lib.rest.CloudControllerClient;
import org.cloudfoundry.client.lib.rest.CloudControllerClientFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.client.CloudFoundryClientExtended;
import com.sap.cloud.lm.sl.cf.client.CloudFoundryTokenProvider;
import com.sap.cloud.lm.sl.cf.client.TokenProvider;
import com.sap.cloud.lm.sl.cf.core.cf.clients.CFOptimizedSpaceGetter;
import com.sap.cloud.lm.sl.cf.core.util.ApplicationConfiguration;
import com.sap.cloud.lm.sl.common.util.Pair;

public class CloudFoundryClientFactory extends ClientFactory {

    @Inject
    private ApplicationConfiguration configuration;

    @Override
    protected Pair<CloudFoundryOperations, TokenProvider> createClient(CloudCredentials credentials) {
        CloudControllerClientFactory factory = new CloudControllerClientFactory(null, configuration.shouldSkipSslValidation());
        addTaggingInterceptor(factory.getRestTemplate());
        OauthClient oauthClient = createOauthClient(factory.getRestTemplate());
        CloudControllerClient controllerClient = factory.newCloudController(configuration.getTargetURL(), credentials, null, oauthClient);
        return new Pair<CloudFoundryOperations, TokenProvider>(new CloudFoundryClientExtended(controllerClient),
            new CloudFoundryTokenProvider(oauthClient));
    }

    @Override
    protected Pair<CloudFoundryOperations, TokenProvider> createClient(CloudCredentials credentials, String org, String space) {
        CloudControllerClientFactory factory = new CloudControllerClientFactory(null, configuration.shouldSkipSslValidation());
        CloudSpace sessionSpace = getSessionSpace(credentials, org, space);
        addTaggingInterceptor(factory.getRestTemplate(), org, space);
        OauthClient oauthClient = createOauthClient(factory.getRestTemplate());
        CloudControllerClient controllerClient = factory.newCloudController(configuration.getTargetURL(), credentials, sessionSpace,
            oauthClient);
        return new Pair<CloudFoundryOperations, TokenProvider>(new CloudFoundryClientExtended(controllerClient),
            new CloudFoundryTokenProvider(oauthClient));
    }

    protected Pair<CloudFoundryOperations, TokenProvider> createClient(CloudCredentials credentials, String spaceId) {
        CloudControllerClientFactory factory = new CloudControllerClientFactory(null, configuration.shouldSkipSslValidation());
        CloudSpace sessionSpace = getSessionSpace(credentials, spaceId);
        addTaggingInterceptor(factory.getRestTemplate(), sessionSpace.getOrganization()
            .getName(), sessionSpace.getName());
        OauthClient oauthClient = createOauthClient(factory.getRestTemplate());
        CloudControllerClient controllerClient = factory.newCloudController(configuration.getTargetURL(), credentials, sessionSpace,
            oauthClient);
        return new Pair<CloudFoundryOperations, TokenProvider>(new CloudFoundryClientExtended(controllerClient),
            new CloudFoundryTokenProvider(oauthClient));
    }

    private void addTaggingInterceptor(RestTemplate template) {
        addTaggingInterceptor(template, null, null);
    }

    private void addTaggingInterceptor(RestTemplate template, String org, String space) {
        if (template.getInterceptors() == null) {
            template.setInterceptors(new ArrayList<>());
        }
        ClientHttpRequestInterceptor requestInterceptor = new TaggingRequestInterceptor(configuration.getVersion(), org, space);
        template.getInterceptors()
            .add(requestInterceptor);
    }

    protected CloudSpace getSessionSpace(CloudCredentials credentials, String orgName, String spaceName) {
        // There are two constructors, which can be used to create a CF client. The first accepts a session space object. The second accepts
        // the org and space names of the session space and attempts to compute it from them. The computation operation is implemented in an
        // incredibly inefficient way, however. This is why here, we create a client without a session space (null) and we use it to compute
        // the session space in a better way (by using the CFOptimizedSpaceGetter). After we do that, we can create a CF client with the
        // computed session space.
        CloudFoundryOperations clientWithoutSessionSpace = createClient(credentials)._1;
        CloudSpace sessionSpace = new CFOptimizedSpaceGetter().findSpace(clientWithoutSessionSpace, orgName, spaceName);
        Assert.notNull(sessionSpace, "No matching organization and space found for org: " + orgName + " space: " + spaceName);
        return sessionSpace;
    }

    protected CloudSpace getSessionSpace(CloudCredentials credentials, String spaceId) {
        // There are two constructors, which can be used to create a CF client. The first accepts a session space object. The second accepts
        // the org and space names of the session space and attempts to compute it from them. The computation operation is implemented in an
        // incredibly inefficient way, however. This is why here, we create a client without a session space (null) and we use it to compute
        // the session space in a better way (by using the CFOptimizedSpaceGetter). After we do that, we can create a CF client with the
        // computed session space.
        CloudFoundryOperations clientWithoutSessionSpace = createClient(credentials)._1;
        CloudSpace sessionSpace = new CFOptimizedSpaceGetter().findSpace(clientWithoutSessionSpace, spaceId);
        Assert.notNull(sessionSpace, "No matching organization and space found for space with id: " + spaceId);
        return sessionSpace;
    }

}

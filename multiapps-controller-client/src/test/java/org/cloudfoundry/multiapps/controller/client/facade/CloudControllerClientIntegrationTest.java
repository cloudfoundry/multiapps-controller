package org.cloudfoundry.multiapps.controller.client.facade;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.multiapps.controller.client.facade.adapters.ImmutableCloudFoundryClientFactory;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableLifecycle;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Lifecycle;
import org.cloudfoundry.multiapps.controller.client.facade.domain.LifecycleType;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.controller.client.facade.rest.CloudSpaceClient;
import org.cloudfoundry.multiapps.controller.client.facade.util.RestUtil;
import org.cloudfoundry.client.CloudFoundryClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.assertNotNull;

abstract class CloudControllerClientIntegrationTest {

    private static final String DEFAULT_CLIENT_ID = "cf";
    private static final String DEFAULT_CLIENT_SECRET = "";
    protected static final String DEFAULT_STACK = "cflinuxfs4";

    protected static CloudControllerClient client;
    protected static CloudFoundryClient delegate;
    protected static CloudSpace target;

    @BeforeAll
    static void login() throws MalformedURLException {
        assertAllRequiredVariablesAreDefined();
        CloudCredentials credentials = getCloudCredentials();
        URL apiUrl = URI.create(ITVariable.CF_API.getValue())
                        .toURL();
        var clientFactory = ImmutableCloudFoundryClientFactory.builder()
                                                              .build();
        var oauthClient = new RestUtil().createOAuthClientByControllerUrl(apiUrl, true);
        oauthClient.init(credentials);
        CloudSpaceClient spaceClient = clientFactory.createSpaceClient(apiUrl, oauthClient, Collections.emptyMap());
        target = spaceClient.getSpace(ITVariable.ORG.getValue(), ITVariable.SPACE.getValue());
        client = new CloudControllerClientImpl(apiUrl, credentials, target, true);
        delegate = clientFactory.createClient(apiUrl, oauthClient, Collections.emptyMap());
    }

    @BeforeEach
    public void setUp(TestInfo testInfo) {
        System.out.println("================================");
        System.out.printf("Test started: %s%n", testInfo.getDisplayName());
    }

    @AfterEach
    public void tearDown(TestInfo testInfo) {
        System.out.printf("Test finished: %s%n", testInfo.getDisplayName());
    }

    protected Lifecycle createLifecycle(Staging staging) {
        if (staging.getDockerInfo() != null) {
            return ImmutableLifecycle.builder()
                                     .type(LifecycleType.DOCKER)
                                     .data(Map.of())
                                     .build();
        }

        var data = new HashMap<String, Object>();
        data.put("buildpacks", staging.getBuildpacks());
        data.put("stack", DEFAULT_STACK);

        // Default to BUILDPACK if lifecycleType is null
        LifecycleType lifecycleType = staging.getLifecycleType() != null
            ? staging.getLifecycleType()
            : LifecycleType.BUILDPACK;

        return ImmutableLifecycle.builder()
                                 .type(lifecycleType)
                                 .data(data)
                                 .build();
    }

    private static void assertAllRequiredVariablesAreDefined() {
        for (ITVariable itVariable : ITVariable.values()) {
            if (!itVariable.isRequired()) {
                continue;
            }
            assertNotNull(itVariable.getValue(), String.format("Missing required value defined by env var %s or system property %s",
                                                               itVariable.getEnvVariable(), itVariable.getProperty()));
        }
    }

    private static CloudCredentials getCloudCredentials() {
        if (ITVariable.USER_ORIGIN.getValue() == null) {
            return new CloudCredentials(ITVariable.USER_EMAIL.getValue(), ITVariable.USER_PASSWORD.getValue());
        }
        return new CloudCredentials(ITVariable.USER_EMAIL.getValue(),
                                    ITVariable.USER_PASSWORD.getValue(),
                                    DEFAULT_CLIENT_ID,
                                    DEFAULT_CLIENT_SECRET,
                                    ITVariable.USER_ORIGIN.getValue());
    }

}

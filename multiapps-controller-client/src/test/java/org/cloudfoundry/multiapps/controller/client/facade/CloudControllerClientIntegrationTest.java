package org.cloudfoundry.multiapps.controller.client.facade;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableLifecycle;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Lifecycle;
import org.cloudfoundry.multiapps.controller.client.facade.domain.LifecycleType;
import org.cloudfoundry.multiapps.controller.client.facade.domain.Staging;
import org.cloudfoundry.multiapps.controller.client.facade.oauth2.OAuthClient;
import org.cloudfoundry.multiapps.controller.client.facade.rest.CloudControllerRestClientFactory;
import org.cloudfoundry.multiapps.controller.client.facade.rest.CloudSpaceClient;
import org.cloudfoundry.multiapps.controller.client.facade.rest.ImmutableCloudControllerRestClientFactory;
import org.cloudfoundry.multiapps.controller.client.facade.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.facade.util.RestUtil;
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
    protected static CloudSpace target;

    @BeforeAll
    static void login() throws MalformedURLException {
        assertAllRequiredVariablesAreDefined();
        CloudCredentials credentials = getCloudCredentials();
        URL apiUrl = URI.create(ITVariable.CF_API.getValue())
                        .toURL();
        CloudControllerRestClientFactory clientFactory = ImmutableCloudControllerRestClientFactory.builder()
                                                                                                  .shouldTrustSelfSignedCertificates(true)
                                                                                                  .build();
        OAuthClient oauthClient = new RestUtil().createOAuthClientByControllerUrl(apiUrl, true);
        oauthClient.init(credentials);
        CloudSpaceClient spaceClient = clientFactory.createSpaceClient(apiUrl, oauthClient, Collections.emptyMap());
        target = spaceClient.getSpace(ITVariable.ORG.getValue(), ITVariable.SPACE.getValue());
        client = new CloudControllerClientImpl(apiUrl, credentials, target, true);
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

        HashMap<String, Object> data = new HashMap<String, Object>();
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

    public static Path getStaticfileApplicationContentPath() {
        URL url = CloudControllerClientIntegrationTest.class.getResource(IntegrationTestConstants.STATICFILE_APPLICATION_CONTENT);

        if (url == null) {
            throw new IllegalStateException(MessageFormat.format("Test resource not found on the classpath: \"{0}\"",
                                                                 IntegrationTestConstants.STATICFILE_APPLICATION_CONTENT));
        }

        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    protected static Map<String, String> getServiceBrokerEnvConfiguration() {
        URL url = CloudControllerClientIntegrationTest.class.getResource(IntegrationTestConstants.SERVICE_BROKER_ENV_CONTENT);
        String configuration;
        try {
            configuration = Files.readString(Paths.get(url.toURI()));
        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException(e);
        }
        return JsonUtil.convertJsonToMap(configuration)
                       .entrySet()
                       .stream()
                       .collect(Collectors.toMap(Map.Entry::getKey, CloudControllerClientIntegrationTest::convertMapEntryValueToString));
    }

    private static String convertMapEntryValueToString(Map.Entry<String, Object> entry) {
        if (entry.getValue() instanceof String string) {
            return string;
        }
        return JsonUtil.convertToJson(entry.getValue(), true);
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

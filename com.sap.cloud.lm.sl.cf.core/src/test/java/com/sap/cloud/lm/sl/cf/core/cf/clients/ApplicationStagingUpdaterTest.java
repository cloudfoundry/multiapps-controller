package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.cloudfoundry.client.lib.domain.Staging;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.core.util.NameUtil;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class ApplicationStagingUpdaterTest {

    private static final String CONTROLLER_ENDPOINT = "https://api.cf.sap.com";
    private static final String V2_APPS_ENDPOINT = "https://api.cf.sap.com/v2/apps/{guid}";
    private RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
    @Mock
    private RestTemplateFactory restTemplateFactory;
    @Mock
    private CloudControllerClient client;
    private ApplicationStagingUpdater applicationStagingUpdater;
    private StepInput input;
    private Staging updateStaging;
    public ApplicationStagingUpdaterTest(String inputLocation) throws Exception {
        this.input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputLocation, getClass()), StepInput.class);
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) With all staging parameters provided
            {
                "application-updater-input-00.json"
            },
            // (1) With some staging parameters provided
            {
                "application-updater-input-01.json"
            },
// @formatter:on
        });
    }

    @Before
    public void setUp() throws MalformedURLException {
        MockitoAnnotations.initMocks(this);
        this.applicationStagingUpdater = new ApplicationStagingUpdater(restTemplateFactory);
        Mockito.when(client.getCloudControllerUrl())
               .thenReturn(new URL(CONTROLLER_ENDPOINT));
        Mockito.when(client.getApplication(input.application.appName))
               .thenReturn(input.application.toCloudApp());
        Mockito.when(restTemplateFactory.getRestTemplate(Mockito.eq(client)))
               .thenReturn(restTemplate);
        updateStaging = input.staging.toStaging();
    }

    @Test
    public void testUpdate() {
        applicationStagingUpdater.updateApplicationStaging(client, input.application.appName, updateStaging);

        validateRestCall();
    }

    private void validateRestCall() {
        Mockito.verify(restTemplate)
               .put(Mockito.eq(V2_APPS_ENDPOINT), Mockito.eq(getStagingMap(updateStaging)), Mockito.eq(input.application.toCloudApp()
                                                                                                                        .getMeta()
                                                                                                                        .getGuid()));
    }

    private Map<String, Object> getStagingMap(Staging staging) {
        Map<String, Object> stagingParameters = new HashMap<>();
        if (staging.getBuildpackUrl() != null) {
            stagingParameters.put("buildpack", staging.getBuildpackUrl());
        }
        if (staging.getCommand() != null) {
            stagingParameters.put("command", staging.getCommand());
        }
        if (staging.getHealthCheckTimeout() != null) {
            stagingParameters.put("health_check_timeout", staging.getHealthCheckTimeout());
        }
        if (staging.getHealthCheckType() != null) {
            stagingParameters.put("health_check_type", staging.getHealthCheckType());
        }
        if (staging.getHealthCheckHttpEndpoint() != null) {
            stagingParameters.put("health_check_http_endpoint", staging.getHealthCheckHttpEndpoint());
        }
        if (staging.isSshEnabled() != null) {
            stagingParameters.put("enable_ssh", staging.isSshEnabled());
        }
        return stagingParameters;
    }

    private static class StepInput {
        SimpleStaging staging;
        SimpleApplication application;
    }

    private static class SimpleStaging {
        String buildpackUrl;
        String command;
        int healthCheckTimeout;
        String healthCheckType;
        String healthCheckHttpEndpoint;
        boolean sshEnabled;

        Staging toStaging() {
            return new Staging.StagingBuilder().command(command)
                                               .buildpackUrl(buildpackUrl)
                                               .healthCheckTimeout(healthCheckTimeout)
                                               .healthCheckType(healthCheckType)
                                               .healthCheckHttpEndpoint(healthCheckHttpEndpoint)
                                               .sshEnabled(sshEnabled)
                                               .build();
        }
    }

    private static class SimpleApplication {
        String appName;

        CloudApplication toCloudApp() {
            return new CloudApplication(new Meta(NameUtil.getUUID(appName), null, null), appName);
        }
    }

}

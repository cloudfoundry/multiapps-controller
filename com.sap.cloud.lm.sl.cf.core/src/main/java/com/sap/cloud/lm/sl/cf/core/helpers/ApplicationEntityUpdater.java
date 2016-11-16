package com.sap.cloud.lm.sl.cf.core.helpers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.client.lib.domain.StagingExtended;

@Component
public class ApplicationEntityUpdater {

    private static final String V2_APPS_ENDPOINT = "/v2/apps/{guid}";
    private static final String HEALTH_CHECK_TYPE_PARAMETER = "health_check_type";
    private static final String HEALTH_CHECK_TIMEOUT_PARAMETER = "health_check_timeout";
    private static final String COMMAND_PARAMETER = "command";
    private static final String BUILDPACK_PARAMETER = "buildpack";

    @Inject
    protected RestTemplateFactory restTemplateFactory;

    public void updateApplicationStaging(CloudFoundryOperations client, String appName, StagingExtended staging) {
        String applicationsEndpoint = getApplicationsEndpoint(client.getCloudControllerUrl().toString());
        CloudApplication application = client.getApplication(appName);
        UUID applicationId = application.getMeta().getGuid();
        Map<String, Object> stagingParameters = createStagingParameters(staging);
        getRestTemplate(client).put(applicationsEndpoint, stagingParameters, applicationId);
    }

    private RestTemplate getRestTemplate(CloudFoundryOperations client) {
        return restTemplateFactory.getRestTemplate(client);
    }

    private String getApplicationsEndpoint(String controllerUrl) {
        return controllerUrl + V2_APPS_ENDPOINT;
    }

    private Map<String, Object> createStagingParameters(StagingExtended staging) {
        Map<String, Object> stagingParameters = new HashMap<>();
        if (staging.getBuildpackUrl() != null) {
            stagingParameters.put(BUILDPACK_PARAMETER, staging.getBuildpackUrl());
        }
        if (staging.getCommand() != null) {
            stagingParameters.put(COMMAND_PARAMETER, staging.getCommand());
        }
        if (staging.getHealthCheckTimeout() != null) {
            stagingParameters.put(HEALTH_CHECK_TIMEOUT_PARAMETER, staging.getHealthCheckTimeout());
        }
        if (staging.getHealthCheckType() != null) {
            stagingParameters.put(HEALTH_CHECK_TYPE_PARAMETER, staging.getHealthCheckType());
        }
        return stagingParameters;
    }

}

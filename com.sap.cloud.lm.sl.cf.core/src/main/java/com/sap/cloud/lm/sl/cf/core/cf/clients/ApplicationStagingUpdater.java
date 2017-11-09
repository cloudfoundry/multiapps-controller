package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.springframework.stereotype.Component;

import com.sap.cloud.lm.sl.cf.client.lib.domain.StagingExtended;

@Component
public class ApplicationStagingUpdater extends CustomControllerClient {

    private static final String V2_APPS_ENDPOINT = "/v2/apps/{guid}";
    private static final String HEALTH_CHECK_TYPE_PARAMETER = "health_check_type";
    private static final String HEALTH_CHECK_HTTP_ENDPOINT_PARAMETER = "health_check_http_endpoint";
    private static final String HEALTH_CHECK_TIMEOUT_PARAMETER = "health_check_timeout";
    private static final String COMMAND_PARAMETER = "command";
    private static final String BUILDPACK_PARAMETER = "buildpack";
    private static final String ENABLE_SSH_PARAMETER = "enable_ssh";

    public void updateApplicationStaging(CloudFoundryOperations client, String appName, StagingExtended staging) {
        new CustomControllerClientErrorHandler().handleErrors(() -> attemptToUpdateApplicationStaging(client, appName, staging));
    }

    private void attemptToUpdateApplicationStaging(CloudFoundryOperations client, String appName, StagingExtended staging) {
        String applicationsEndpoint = getApplicationsEndpoint(client.getCloudControllerUrl().toString());
        CloudApplication application = client.getApplication(appName);
        UUID applicationId = application.getMeta().getGuid();
        Map<String, Object> stagingParameters = createStagingParameters(staging);
        getRestTemplate(client).put(applicationsEndpoint, stagingParameters, applicationId);
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
        if (staging.getHealthCheckHttpEndpoint() != null) {
            stagingParameters.put(HEALTH_CHECK_HTTP_ENDPOINT_PARAMETER, staging.getHealthCheckHttpEndpoint());
        }
        if (staging.isSshEnabled() != null) {
            stagingParameters.put(ENABLE_SSH_PARAMETER, staging.isSshEnabled());
        }
        return stagingParameters;
    }

}

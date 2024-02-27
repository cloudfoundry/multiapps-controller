package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.Staging;
import org.springframework.stereotype.Component;

@Component
public class ApplicationStagingUpdater extends CustomControllerClient {

    private static final String V2_APPS_ENDPOINT = "/v2/apps/{guid}";
    private static final String HEALTH_CHECK_TYPE_PARAMETER = "health_check_type";
    private static final String HEALTH_CHECK_HTTP_ENDPOINT_PARAMETER = "health_check_http_endpoint";
    private static final String HEALTH_CHECK_TIMEOUT_PARAMETER = "health_check_timeout";
    private static final String COMMAND_PARAMETER = "command";
    private static final String BUILDPACK_PARAMETER = "buildpack";
    private static final String ENABLE_SSH_PARAMETER = "enable_ssh";
    private static final String DOCKER_IMAGE = "docker_image";
    private static final String DOCKER_CREDENTIALS = "docker_credentials";

    @Inject
    public ApplicationStagingUpdater(RestTemplateFactory restTemplateFactory) {
        super(restTemplateFactory);
    }

    public void updateApplicationStaging(CloudControllerClient client, String appName, Staging staging) {
        new CustomControllerClientErrorHandler().handleErrors(() -> attemptToUpdateApplicationStaging(client, appName, staging));
    }

    private void attemptToUpdateApplicationStaging(CloudControllerClient client, String appName, Staging staging) {
        String applicationsEndpoint = getApplicationsEndpoint(client.getCloudControllerUrl()
                                                                    .toString());
        CloudApplication application = client.getApplication(appName);
        UUID applicationId = application.getMeta()
                                        .getGuid();
        Map<String, Object> stagingParameters = createStagingParameters(staging);
        getRestTemplate(client).put(applicationsEndpoint, stagingParameters, applicationId);
    }

    private String getApplicationsEndpoint(String controllerUrl) {
        return controllerUrl + V2_APPS_ENDPOINT;
    }

    private Map<String, Object> createStagingParameters(Staging staging) {
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
        if (staging.getDockerInfo() != null) {
            stagingParameters.put(DOCKER_IMAGE, staging.getDockerInfo()
                                                       .getImage());
            if (staging.getDockerInfo()
                       .getDockerCredentials() != null) {
                stagingParameters.put(DOCKER_CREDENTIALS, staging.getDockerInfo()
                                                                 .getDockerCredentials());
            }
        }
        return stagingParameters;
    }

}

package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.util.CloudEntityResourceMapper;
import org.springframework.web.client.RestTemplate;

import com.sap.cloud.lm.sl.cf.core.cf.apps.ApplicationStagingState;
import com.sap.cloud.lm.sl.common.util.JsonUtil;

public class ApplicationStagingStateGetter extends CustomControllerClient {

    private static final String STAGING_STATE_ATTRIBUTE_NAME = "package_state";

    public ApplicationStagingState getApplicationStagingState(CloudFoundryOperations client, String appName) {
        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(
            () -> attemptToGetApplicationStagingState(client, appName));
    }

    private ApplicationStagingState attemptToGetApplicationStagingState(CloudFoundryOperations client, String appName) {
        CloudApplication app = client.getApplication(appName);
        String appUrl = getAppUrl(app.getMeta().getGuid());
        return doGetApplicationStagingState(client, appUrl);
    }

    private String getAppUrl(UUID appGuid) {
        return "/v2/apps/" + appGuid.toString();
    }

    private ApplicationStagingState doGetApplicationStagingState(CloudFoundryOperations client, String appUrl) {
        RestTemplate restTemplate = getRestTemplate(client);
        String cloudControllerUrl = client.getCloudControllerUrl().toString();
        String response = restTemplate.getForObject(getUrl(cloudControllerUrl, appUrl), String.class);
        return parseApplicationStagingState(response);
    }

    private ApplicationStagingState parseApplicationStagingState(String response) {
        Map<String, Object> parsedResponse = JsonUtil.convertJsonToMap(response);
        String applicationStagingState = CloudEntityResourceMapper.getEntityAttribute(parsedResponse, STAGING_STATE_ATTRIBUTE_NAME,
            String.class);
        return applicationStagingState == null ? null : ApplicationStagingState.valueOf(applicationStagingState);
    }

}

package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.util.CloudEntityResourceMapper;
import org.springframework.web.client.RestTemplate;

public class ApplicationRoutesGetter extends CustomControllerClient {

    private CloudEntityResourceMapper resourceMapper = new CloudEntityResourceMapper();

    public List<CloudRoute> getRoutes(CloudFoundryOperations client, String appName) {
        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(() -> attemptToGetRoutes(client, appName));
    }

    private List<CloudRoute> attemptToGetRoutes(CloudFoundryOperations client, String appName) {
        CloudApplication app = client.getApplication(appName);
        String appRoutesUrl = getAppRoutesUrl(app.getMeta()
            .getGuid());
        return doGetRoutes(client, appRoutesUrl);
    }

    private String getAppRoutesUrl(UUID appGuid) {
        return "/v2/apps/" + appGuid.toString() + "/routes?inline-relations-depth=1";
    }

    private List<CloudRoute> doGetRoutes(CloudFoundryOperations client, String appRoutesUrl) {
        RestTemplate restTemplate = getRestTemplate(client);
        String cloudControllerUrl = client.getCloudControllerUrl()
            .toString();
        List<Map<String, Object>> resources = getAllResources(restTemplate, cloudControllerUrl, appRoutesUrl);
        return toCloudRoutes(resources);
    }

    private List<CloudRoute> toCloudRoutes(List<Map<String, Object>> resources) {
        return resources.stream()
            .map(resource -> resourceMapper.mapResource(resource, CloudRoute.class))
            .collect(Collectors.toList());
    }

}

package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.util.CloudEntityResourceMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.web.client.RestTemplate;

@Named
@Profile("cf")
public class ApplicationRoutesGetter extends CustomControllerClient {

    private CloudEntityResourceMapper resourceMapper = new CloudEntityResourceMapper();

    @Inject
    public ApplicationRoutesGetter(RestTemplateFactory restTemplateFactory) {
        super(restTemplateFactory);
    }

    public List<CloudRoute> getRoutes(CloudControllerClient client, String appName) {
        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(() -> attemptToGetRoutes(client, appName));
    }

    private List<CloudRoute> attemptToGetRoutes(CloudControllerClient client, String appName) {
        CloudApplication app = client.getApplication(appName);
        String appRoutesUrl = getAppRoutesUrl(app.getMeta()
                                                 .getGuid());
        return doGetRoutes(client, appRoutesUrl);
    }

    private String getAppRoutesUrl(UUID appGuid) {
        return "/v2/apps/" + appGuid.toString() + "/routes?inline-relations-depth=1";
    }

    private List<CloudRoute> doGetRoutes(CloudControllerClient client, String appRoutesUrl) {
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

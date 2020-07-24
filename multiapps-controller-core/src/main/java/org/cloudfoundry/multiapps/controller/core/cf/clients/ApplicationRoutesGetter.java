package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.springframework.web.client.RestTemplate;

@Named
public class ApplicationRoutesGetter extends CustomControllerClient {

    private final CloudEntityResourceMapper resourceMapper = new CloudEntityResourceMapper();

    @Inject
    public ApplicationRoutesGetter(RestTemplateFactory restTemplateFactory) {
        super(restTemplateFactory);
    }

    public List<CloudRoute> getRoutes(CloudControllerClient client, String appName) {
        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(() -> attemptToGetRoutes(client, appName));
    }

    private List<CloudRoute> attemptToGetRoutes(CloudControllerClient client, String appName) {
        CloudApplication app = client.getApplication(appName);
        String appRoutesUrl = getAppRoutesUrl(app.getMetadata()
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
                        .map(resourceMapper::mapRouteResource)
                        .collect(Collectors.toList());
    }

}

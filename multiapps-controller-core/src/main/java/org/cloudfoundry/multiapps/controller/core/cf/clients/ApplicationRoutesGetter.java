package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;

public class ApplicationRoutesGetter extends CustomControllerClient {

    private final CloudEntityResourceMapper resourceMapper = new CloudEntityResourceMapper();

    public ApplicationRoutesGetter(CloudControllerClient client) {
        super(client);
    }

    public List<CloudRoute> getRoutes(String appName) {
        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(() -> getRoutesInternal(appName));
    }

    private List<CloudRoute> getRoutesInternal(String appName) {
        CloudApplication app = client.getApplication(appName);
        String appRoutesUrl = getAppRoutesUrl(app.getMetadata()
                                                 .getGuid());
        List<Map<String, Object>> resources = getAllResources(appRoutesUrl);
        return toCloudRoutes(resources);
    }

    private String getAppRoutesUrl(UUID appGuid) {
        return "/v2/apps/" + appGuid.toString() + "/routes?inline-relations-depth=1";
    }

    private List<CloudRoute> toCloudRoutes(List<Map<String, Object>> resources) {
        return resources.stream()
                        .map(resourceMapper::mapRouteResource)
                        .collect(Collectors.toList());
    }

}

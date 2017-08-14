package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.springframework.web.client.RestTemplate;

public class DefaultTagsDetector extends CustomControllerClient {

    private static final String SERVICES_ENDPOINT = "/v2/services";

    public Map<String, List<String>> computeDefaultTags(CloudFoundryOperations client) {
        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(() -> attemptToComputeDefaultTags(client));
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> attemptToComputeDefaultTags(CloudFoundryOperations client) {
        String controllerUrl = client.getCloudControllerUrl().toString();
        RestTemplate restTemplate = getRestTemplate(client);
        List<Map<String, Object>> resources = getAllResources(restTemplate, controllerUrl, SERVICES_ENDPOINT);
        Map<String, List<String>> defaultTags = new HashMap<>();
        for (Map<String, Object> resource : resources) {
            Map<String, Object> service = (Map<String, Object>) resource.get("entity");
            String label = (String) service.get("label");
            List<String> tags = (List<String>) service.get("tags");
            defaultTags.put(label, tags);
        }
        return defaultTags;
    }

}

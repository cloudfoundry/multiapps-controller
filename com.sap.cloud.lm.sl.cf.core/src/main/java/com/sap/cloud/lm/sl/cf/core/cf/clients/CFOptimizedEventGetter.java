package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudControllerClient;

public class CFOptimizedEventGetter extends CustomControllerClient {

    private static final String FIND_EVENT_BY_TYPE_AND_TIMESTAMP_ENDPOINT = "/v2/events?inline-relations-depth=1&results-per-page=100&q=type:{type}&q=timestamp>{timestamp}";
    private CloudControllerClient client;

    public CFOptimizedEventGetter(CloudControllerClient client) {
        super(new RestTemplateFactory());
        this.client = client;
    }

    public List<String> findEvents(String type, String timestamp) {
        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(() -> findEventsInternal(type, timestamp));
    }

    private List<String> findEventsInternal(String type, String timestamp) {
        Map<String, Object> urlVariables = getAsUrlVariablesForFindEventsRequest(type, timestamp);
        return executeFindEventsRequest(urlVariables);
    }

    private Map<String, Object> getAsUrlVariablesForFindEventsRequest(String type, String timestamp) {
        Map<String, Object> result = new HashMap<>();
        result.put("type", type);
        result.put("timestamp", timestamp);
        return result;
    }

    private List<String> executeFindEventsRequest(Map<String, Object> urlVariables) {
        String controllerUrl = client.getCloudControllerUrl()
            .toString();
        List<Map<String, Object>> response = getAllResources(getRestTemplate(client), controllerUrl,
            FIND_EVENT_BY_TYPE_AND_TIMESTAMP_ENDPOINT, urlVariables);
        return extractSpaceIds(response);
    }

    private List<String> extractSpaceIds(List<Map<String, Object>> events) {
        return events.stream()
            .map(event -> extractSpaceId(event))
            .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private String extractSpaceId(Map<String, Object> event) {
        Map<String, Object> entity = (Map<String, Object>) event.get("entity");
        return (String) entity.get("space_guid");
    }

}

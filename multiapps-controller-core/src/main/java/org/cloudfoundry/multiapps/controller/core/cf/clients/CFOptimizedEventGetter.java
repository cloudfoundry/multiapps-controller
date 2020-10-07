package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudControllerClient;

public class CFOptimizedEventGetter extends CustomControllerClient {

    private static final String FIND_EVENT_BY_TYPE_AND_TIMESTAMP_ENDPOINT = "/v2/events?inline-relations-depth=1&results-per-page=100&q=type:{type}&q=timestamp>{timestamp}";

    public CFOptimizedEventGetter(CloudControllerClient client) {
        super(client);
    }

    public List<String> findEvents(String type, String timestamp) {
        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(() -> findEventsInternal(type, timestamp));
    }

    private List<String> findEventsInternal(String type, String timestamp) {
        List<Map<String, Object>> response = getAllResources(FIND_EVENT_BY_TYPE_AND_TIMESTAMP_ENDPOINT, type, timestamp);
        return extractSpaceIds(response);
    }

    private List<String> extractSpaceIds(List<Map<String, Object>> events) {
        return events.stream()
                     .map(this::extractSpaceId)
                     .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private String extractSpaceId(Map<String, Object> event) {
        Map<String, Object> entity = (Map<String, Object>) event.get("entity");
        return (String) entity.get("space_guid");
    }

}

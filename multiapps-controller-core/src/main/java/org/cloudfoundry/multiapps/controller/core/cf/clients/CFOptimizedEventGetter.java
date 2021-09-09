package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;

public class CFOptimizedEventGetter extends CustomControllerClient {

    private static final String FIND_EVENT_BY_TYPE_AND_TIMESTAMP_ENDPOINT = "/v3/audit_events?types=%s&per_page=100&created_ats[gt]=%s";

    public CFOptimizedEventGetter(CloudControllerClient client) {
        super(client);
    }

    public List<String> findEvents(String type, String timestamp) {
        return new CustomControllerClientErrorHandler().handleErrorsOrReturnResult(() -> findEventsInternal(type, timestamp));
    }

    private List<String> findEventsInternal(String type, String timestamp) {
        List<Map<String, Object>> response = getAllResources(String.format(FIND_EVENT_BY_TYPE_AND_TIMESTAMP_ENDPOINT, type, timestamp));
        return extractSpaceIds(response);
    }

    private List<String> extractSpaceIds(List<Map<String, Object>> events) {
        return events.stream()
                     .map(this::extractSpaceId)
                     .filter(Objects::nonNull)
                     .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private String extractSpaceId(Map<String, Object> event) {
        Map<String, Object> space = (Map<String, Object>) event.get("space");
        return (String) space.get("guid");
    }

}

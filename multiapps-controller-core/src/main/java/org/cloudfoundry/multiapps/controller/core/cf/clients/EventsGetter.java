package org.cloudfoundry.multiapps.controller.core.cf.clients;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.springframework.web.reactive.function.client.WebClient;

@Named
public class EventsGetter extends CustomControllerClient {

    private static final String GUID = "guid";
    private static final String EVENTS_URL = "/v2/events?q=actee:{guid}&order-by:timestamp&order-direction=desc";

    private static final String USER_PROVIDED_SERVICE_EVENT_TYPE_DELETE = "audit.user_provided_service_instance.delete";
    private static final String SERVICE_EVENT_TYPE_DELETE = "audit.service_instance.delete";

    private final CloudEntityResourceMapper resourceMapper = new CloudEntityResourceMapper();

    @Inject
    public EventsGetter(WebClientFactory webClientFactory) {
        super(webClientFactory);
    }

    private Map<String, Object> buildQueryParameters(UUID uuid) {
        Map<String, Object> urlVars = new HashMap<>();
        urlVars.put(GUID, uuid);
        return urlVars;
    }

    public List<CloudEvent> getEvents(UUID uuid, CloudControllerClient client) {
        Map<String, Object> queryParames = buildQueryParameters(uuid);
        String controllerUrl = client.getCloudControllerUrl()
                                     .toString();

        WebClient webClient = getWebClient(client);

        List<Map<String, Object>> resources = getAllResources(webClient, controllerUrl, EVENTS_URL, queryParames);

        return resources.stream()
                        .filter(Objects::nonNull)
                        .map(resourceMapper::mapEventResource)
                        .collect(Collectors.toList());
    }

    public CloudEvent getLastEvent(UUID uuid, CloudControllerClient client) {
        List<CloudEvent> events = getEvents(uuid, client);
        return events.isEmpty() ? null : events.get(0);
    }

    public boolean isDeleteEvent(String eventType) {
        return SERVICE_EVENT_TYPE_DELETE.equalsIgnoreCase(eventType) || USER_PROVIDED_SERVICE_EVENT_TYPE_DELETE.equalsIgnoreCase(eventType);
    }
}

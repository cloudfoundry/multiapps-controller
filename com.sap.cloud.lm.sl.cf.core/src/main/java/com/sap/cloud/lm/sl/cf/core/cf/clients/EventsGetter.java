package com.sap.cloud.lm.sl.cf.core.cf.clients;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.util.CloudEntityResourceMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class EventsGetter extends CustomControllerClient {

    private static final String GUID = "guid";
    private static final String EVENTS_URL = "/v2/events?q=actee:{guid}&order-by:timestamp&order-direction=desc";
    private CloudEntityResourceMapper resourceMapper = new CloudEntityResourceMapper();

    @Inject
    public EventsGetter(RestTemplateFactory restTemplateFactory) {
        super(restTemplateFactory);
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

        RestTemplate restTemplate = getRestTemplate(client);

        List<Map<String, Object>> resources = getAllResources(restTemplate, controllerUrl, EVENTS_URL, queryParames);

        return resources.stream()
            .filter(Objects::nonNull)
            .map(map -> resourceMapper.mapResource(map, CloudEvent.class))
            .collect(Collectors.toList());
    }
    
    public CloudEvent getLastEvent(UUID uuid, CloudControllerClient client) {
        List<CloudEvent> events = getEvents(uuid, client);
        return events.isEmpty() ? null : events.get(0);
    }

}

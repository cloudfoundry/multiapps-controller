package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEvent;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3AuditEvent;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3AuditEventMapper;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3QueryParameters;
import org.springframework.core.ParameterizedTypeReference;

public class EventsV3Operations {

    private static final ParameterizedTypeReference<V3ListResponse<V3AuditEvent>> EVENT_PAGE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client client;

    private final CloudSpace targetSpace;

    public EventsV3Operations(CloudControllerV3Client client, CloudSpace targetSpace) {
        this.client = client;
        this.targetSpace = targetSpace;
    }

    public List<CloudEvent> getEvents() {
        return client.list(V3Uri.path(CloudControllerV3Endpoints.AUDIT_EVENTS)
                                .perPage(CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE)
                                .build(), EVENT_PAGE)
                     .stream()
                     .map(V3AuditEventMapper::toCloudEvent)
                     .toList();
    }

    public List<CloudEvent> getEventsByTarget(UUID uuid) {
        String query = V3Uri.path(CloudControllerV3Endpoints.AUDIT_EVENTS)
                            .perPage(CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE)
                            .query(V3QueryParameters.TARGET_GUIDS, uuid)
                            .build();

        return client.list(query, EVENT_PAGE)
                     .stream()
                     .map(V3AuditEventMapper::toCloudEvent)
                     .toList();
    }

    public List<CloudEvent> getApplicationEvents(String applicationName) {
        UUID applicationGuid = OperationsUtil.getRequiredApplicationGuid(client, targetSpace, applicationName);

        return getEventsByTarget(applicationGuid);
    }

}

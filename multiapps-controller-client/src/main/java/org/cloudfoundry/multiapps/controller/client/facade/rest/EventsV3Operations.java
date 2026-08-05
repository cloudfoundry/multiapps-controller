package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEvent;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Application;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3AuditEvent;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3AuditEventMapper;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;

/**
 * CF v3 <em>audit event</em> operations for the cf-java-client replacement. Reproduces the HTTP shape and domain-mapping of the OSS
 * {@code CloudControllerRestClientImpl} event methods:
 * <ul>
 * <li>{@code getEvents()} &rarr; {@code GET /v3/audit_events} (paginated);</li>
 * <li>{@code getEventsByTarget(uuid)} &rarr; {@code GET /v3/audit_events?target_guids=<uuid>} (paginated);</li>
 * <li>{@code getApplicationEvents(name)} &rarr; resolve the app guid in the target space, then {@code getEventsByTarget}.</li>
 * </ul>
 * Audit events are a global resource, so — like the OSS impl — the listings are not filtered by the target space (only by target guid where
 * requested). Application-name resolution mirrors the OSS impl's {@code getRequiredApplicationGuid}: it queries the target space and throws
 * {@link CloudOperationException} with {@link HttpStatus#NOT_FOUND} when the application does not exist.
 */
public class EventsV3Operations {

    // CF v3 caps per_page at 5000; use a large page to minimise round-trips (the pagination walker still handles multiple pages).
    private static final int DEFAULT_PAGE_SIZE = 5000;

    private static final ParameterizedTypeReference<V3ListResponse<V3AuditEvent>> EVENT_PAGE = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<V3ListResponse<V3Application>> APPLICATION_PAGE = new ParameterizedTypeReference<>() {
    };

    private final CloudControllerV3Client cc;
    private final CloudSpace target;

    public EventsV3Operations(CloudControllerV3Client cc, CloudSpace target) {
        this.cc = cc;
        this.target = target;
    }

    public List<CloudEvent> getEvents() {
        return cc.list("/v3/audit_events?per_page=" + DEFAULT_PAGE_SIZE, EVENT_PAGE)
                 .stream()
                 .map(V3AuditEventMapper::toCloudEvent)
                 .toList();
    }

    public List<CloudEvent> getEventsByTarget(UUID uuid) {
        String query = "/v3/audit_events?per_page=" + DEFAULT_PAGE_SIZE + "&target_guids=" + uuid;
        return cc.list(query, EVENT_PAGE)
                 .stream()
                 .map(V3AuditEventMapper::toCloudEvent)
                 .toList();
    }

    public List<CloudEvent> getApplicationEvents(String applicationName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        return getEventsByTarget(applicationGuid);
    }

    // Mirrors the OSS impl's getRequiredApplicationGuid: look up the app in the target space and 404 if it is absent.
    private UUID getRequiredApplicationGuid(String applicationName) {
        StringBuilder query = new StringBuilder("/v3/apps?per_page=" + DEFAULT_PAGE_SIZE);
        if (target != null && target.getGuid() != null) {
            query.append("&space_guids=")
                 .append(target.getGuid());
        }
        query.append("&names=")
             .append(applicationName);
        List<V3Application> apps = cc.list(query.toString(), APPLICATION_PAGE);
        if (apps.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Application " + applicationName + " not found.");
        }
        return UUID.fromString(apps.get(0)
                                   .guid());
    }

}

package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.Messages;
import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEvent;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Application;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3AuditEvent;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3AuditEventMapper;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;

public class EventsV3Operations {

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
        return cc.list(CloudControllerV3Endpoints.AUDIT_EVENTS + CloudControllerV3Endpoints.QUERY_PER_PAGE
                           + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE, EVENT_PAGE)
                 .stream()
                 .map(V3AuditEventMapper::toCloudEvent)
                 .toList();
    }

    public List<CloudEvent> getEventsByTarget(UUID uuid) {
        String query = CloudControllerV3Endpoints.AUDIT_EVENTS + CloudControllerV3Endpoints.QUERY_PER_PAGE
            + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE + CloudControllerV3Endpoints.AMPERSAND_TARGET_GUIDS + uuid;

        return cc.list(query, EVENT_PAGE)
                 .stream()
                 .map(V3AuditEventMapper::toCloudEvent)
                 .toList();
    }

    public List<CloudEvent> getApplicationEvents(String applicationName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);

        return getEventsByTarget(applicationGuid);
    }

    private UUID getRequiredApplicationGuid(String applicationName) {
        StringBuilder query = new StringBuilder(CloudControllerV3Endpoints.APPS + CloudControllerV3Endpoints.QUERY_PER_PAGE
                                                    + CloudControllerV3Endpoints.DEFAULT_PAGE_SIZE);

        if (target != null && target.getGuid() != null) {
            query.append(CloudControllerV3Endpoints.AMPERSAND_SPACE_GUIDS)
                 .append(target.getGuid());
        }

        query.append(CloudControllerV3Endpoints.AMPERSAND_NAMES)
             .append(applicationName);
        List<V3Application> apps = cc.list(query.toString(), APPLICATION_PAGE);

        if (apps.isEmpty()) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, Messages.NOT_FOUND,
                                              MessageFormat.format(Messages.APPLICATION_0_NOT_FOUND, applicationName));
        }

        return UUID.fromString(apps.getFirst()
                                   .guid());
    }

}

package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEvent;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEvent.Participant;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudEvent;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudEvent.ImmutableParticipant;

/**
 * Maps the {@link V3AuditEvent} wire model to the project's {@link CloudEvent} domain object. Mirrors the OSS {@code RawCloudEvent} adapter
 * field-for-field, so both client implementations yield identical domain objects.
 * <p>
 * As in the OSS adapter, a {@code null} actor/target still yields an empty (non-{@code null}) {@link Participant}.
 */
public final class V3AuditEventMapper {

    private V3AuditEventMapper() {
    }

    public static CloudEvent toCloudEvent(V3AuditEvent event) {
        return ImmutableCloudEvent.builder()
                                  .metadata(V3ResourceMappers.parseMetadata(event.guid(), event.createdAt(), event.updatedAt()))
                                  .target(parseParticipant(event.target()))
                                  .actor(parseParticipant(event.actor()))
                                  .type(event.type())
                                  .build();
    }

    private static Participant parseParticipant(V3AuditEvent.V3Participant participant) {
        if (participant == null) {
            return ImmutableParticipant.builder()
                                       .build();
        }
        return ImmutableParticipant.builder()
                                   .guid(V3ResourceMappers.parseNullableGuid(participant.guid()))
                                   .name(participant.name())
                                   .type(participant.type())
                                   .build();
    }

}

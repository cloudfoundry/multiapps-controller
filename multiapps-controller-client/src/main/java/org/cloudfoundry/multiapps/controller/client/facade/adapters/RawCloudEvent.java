package org.cloudfoundry.multiapps.controller.client.facade.adapters;

import org.cloudfoundry.client.v3.auditevents.AuditEventActor;
import org.cloudfoundry.client.v3.auditevents.AuditEventResource;
import org.cloudfoundry.client.v3.auditevents.AuditEventTarget;
import org.immutables.value.Value;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEvent;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEvent.Participant;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudEvent;
import org.cloudfoundry.multiapps.controller.client.facade.domain.ImmutableCloudEvent.ImmutableParticipant;

@Value.Immutable
public abstract class RawCloudEvent extends RawCloudEntity<CloudEvent> {

    @Value.Parameter
    public abstract AuditEventResource getResource();

    @Override
    public CloudEvent derive() {
        AuditEventResource resource = getResource();
        return ImmutableCloudEvent.builder()
                                  .metadata(parseResourceMetadata(resource))
                                  .target(parseTarget(resource))
                                  .actor(parseActor(resource))
                                  .type(resource.getType())
                                  .build();
    }

    private static Participant parseTarget(AuditEventResource resource) {
        AuditEventTarget target = resource.getAuditEventTarget();
        if (target == null) {
            return ImmutableParticipant.builder()
                                       .build();
        }
        return ImmutableParticipant.builder()
                                   .guid(parseNullableGuid(target.getId()))
                                   .name(target.getName())
                                   .type(target.getType())
                                   .build();
    }

    private static Participant parseActor(AuditEventResource resource) {
        AuditEventActor actor = resource.getAuditEventActor();
        if (actor == null) {
            return ImmutableParticipant.builder()
                                       .build();
        }
        return ImmutableParticipant.builder()
                                   .guid(parseNullableGuid(actor.getId()))
                                   .name(actor.getName())
                                   .type(actor.getType())
                                   .build();
    }

}

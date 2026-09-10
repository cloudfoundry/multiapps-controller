package org.cloudfoundry.multiapps.controller.client.facade.rest.resources;

import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEvent;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3AuditEvent.V3Participant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class V3AuditEventMapperTest {

    private static final String GUID_STRING = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";
    private static final String ACTOR_GUID = "11111111-2222-3333-4444-555555555555";
    private static final String TARGET_GUID = "99999999-8888-7777-6666-555555555555";

    @Test
    void testToCloudEventMapsAllFields() {
        V3AuditEvent event = new V3AuditEvent(GUID_STRING, "2026-08-04T10:15:30Z", null, "audit.app.update",
                                              new V3Participant(ACTOR_GUID, "user", "admin"),
                                              new V3Participant(TARGET_GUID, "app", "my-app"));

        CloudEvent result = V3AuditEventMapper.toCloudEvent(event);

        Assertions.assertEquals("audit.app.update", result.getType());
        Assertions.assertEquals(ACTOR_GUID, result.getActor()
                                                  .getGuid()
                                                  .toString());
        Assertions.assertEquals("admin", result.getActor()
                                               .getName());
        Assertions.assertEquals("user", result.getActor()
                                              .getType());
        Assertions.assertEquals("my-app", result.getTarget()
                                                .getName());
    }

    @Test
    void testToCloudEventNullActorReturnsEmptyParticipant() {
        V3AuditEvent event = new V3AuditEvent(GUID_STRING, null, null, "audit.app.update", null,
                                              new V3Participant(TARGET_GUID, "app", "my-app"));

        CloudEvent result = V3AuditEventMapper.toCloudEvent(event);

        Assertions.assertNotNull(result.getActor());
        Assertions.assertNull(result.getActor()
                                    .getGuid());
        Assertions.assertNull(result.getActor()
                                    .getName());
    }

    @Test
    void testToCloudEventNullTargetReturnsEmptyParticipant() {
        V3AuditEvent event = new V3AuditEvent(GUID_STRING, null, null, "audit.app.update",
                                              new V3Participant(ACTOR_GUID, "user", "admin"), null);

        CloudEvent result = V3AuditEventMapper.toCloudEvent(event);

        Assertions.assertNotNull(result.getTarget());
        Assertions.assertNull(result.getTarget()
                                    .getGuid());
    }

}

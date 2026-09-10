package org.cloudfoundry.multiapps.controller.client.facade.rest;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.multiapps.controller.client.facade.CloudOperationException;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudEvent;
import org.cloudfoundry.multiapps.controller.client.facade.domain.CloudSpace;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3Application;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3AuditEvent;
import org.cloudfoundry.multiapps.controller.client.facade.rest.resources.V3ListResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;

class EventsV3OperationsTest {

    private static final String GUID_STRING = "3725a721-6e3f-4b6e-8f2b-1c2d3e4f5a6b";
    private static final UUID SPACE_GUID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID TARGET_GUID = UUID.fromString("99999999-8888-7777-6666-555555555555");

    @Mock
    private CloudControllerV3Client cc;
    @Mock
    private CloudSpace target;

    private EventsV3Operations operations;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        operations = new EventsV3Operations(cc, target);
    }

    @Test
    void testGetEventsMapsResults() {
        Mockito.when(
                   cc.list(ArgumentMatchers.anyString(), ArgumentMatchers.<ParameterizedTypeReference<V3ListResponse<V3AuditEvent>>> any()))
               .thenReturn(List.of(getAuditEvent("audit.app.update"), getAuditEvent("audit.app.create")));

        List<CloudEvent> result = operations.getEvents();

        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("audit.app.update", result.get(0)
                                                          .getType());
    }

    @Test
    void testGetEventsByTargetIncludesTargetGuidInQuery() {
        Mockito.when(
                   cc.list(ArgumentMatchers.anyString(), ArgumentMatchers.<ParameterizedTypeReference<V3ListResponse<V3AuditEvent>>> any()))
               .thenReturn(List.of());

        operations.getEventsByTarget(TARGET_GUID);

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(cc)
               .list(uriCaptor.capture(), ArgumentMatchers.<ParameterizedTypeReference<V3ListResponse<V3AuditEvent>>> any());
        Assertions.assertTrue(uriCaptor.getValue()
                                       .contains("/v3/audit_events"), uriCaptor.getValue());
        Assertions.assertTrue(uriCaptor.getValue()
                                       .contains("target_guids=" + TARGET_GUID), uriCaptor.getValue());
    }

    @Test
    void testGetApplicationEventsThrowsWhenApplicationNotFound() {
        Mockito.when(target.getGuid())
               .thenReturn(SPACE_GUID);

        Mockito.when(cc.list(ArgumentMatchers.contains("/v3/apps"),
                             ArgumentMatchers.<ParameterizedTypeReference<V3ListResponse<V3Application>>> any()))
               .thenReturn(List.of());

        CloudOperationException thrown = Assertions.assertThrows(CloudOperationException.class,
                                                                 () -> operations.getApplicationEvents("missing-app"));

        Assertions.assertEquals(HttpStatus.NOT_FOUND, thrown.getStatusCode());
    }

    @Test
    void testGetApplicationEventsQueryScopesAppsToSpaceWhenTargetPresent() {
        Mockito.when(target.getGuid())
               .thenReturn(SPACE_GUID);
        Mockito.when(cc.list(ArgumentMatchers.contains("/v3/apps"),
                             ArgumentMatchers.<ParameterizedTypeReference<V3ListResponse<V3Application>>> any()))
               .thenReturn(List.of(new V3Application(GUID_STRING, "my-app", "STARTED", null, null, null, null, null)));
        Mockito.when(cc.list(ArgumentMatchers.contains("/v3/audit_events"),
                             ArgumentMatchers.<ParameterizedTypeReference<V3ListResponse<V3AuditEvent>>> any()))
               .thenReturn(List.of());

        operations.getApplicationEvents("my-app");

        ArgumentCaptor<String> uriCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(cc, Mockito.times(2))
               .list(uriCaptor.capture(), ArgumentMatchers.<ParameterizedTypeReference<V3ListResponse<V3Application>>> any());
        String appsQuery = uriCaptor.getAllValues()
                                    .stream()
                                    .filter(uri -> uri.contains("/v3/apps"))
                                    .findFirst()
                                    .orElseThrow();
        Assertions.assertTrue(appsQuery.contains("space_guids=" + SPACE_GUID), appsQuery);
        Assertions.assertTrue(appsQuery.contains("names=my-app"), appsQuery);
    }

    private static V3AuditEvent getAuditEvent(String type) {
        return new V3AuditEvent(GUID_STRING, null, null, type, new V3AuditEvent.V3Participant(GUID_STRING, "user", "admin"),
                                new V3AuditEvent.V3Participant(GUID_STRING, "app", "my-app"));
    }

}

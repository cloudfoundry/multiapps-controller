package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.core.cf.clients.EventsGetter;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceGetter;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceInstanceGetter;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.common.SLException;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class DeleteServicesStepTest extends SyncFlowableStepTest<DeleteServicesStep> {

    private static final String POLLING = "polling";
    private static final String STEP_EXECUTION = "stepExecution";
    private static final String TEST_SPACE_ID = "testSpace";
    private static final String SERVICE_EVENT_TYPE_DELETE = "audit.service_instance.delete";

    private final StepInput stepInput;
    private final String expectedExceptionMessage;

    private List<String> servicesToDelete = new ArrayList<>();
    private Map<String, String> servicesGuids = new HashMap<>();

    @Mock
    protected CloudControllerClient client;

    @Mock
    private ServiceGetter serviceGetter;

    @Mock
    private EventsGetter eventsGetter;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
        // @formatter:off
            // (0) Services have bindings:
            {
                "delete-services-step-input-1.json", null,
            },
            // (1) Services do not have bindings:
            {
                "delete-services-step-input-2.json", null,
            },
            // (2) No services to delete:
            {
                "delete-services-step-input-3.json", null,
            },
            // (3) Some services have bindings, some do not but are deleted slowly:
            {
                "delete-services-step-input-4.json", null,
            },
            // (4) One of the services is missing (maybe the user deleted it manually):
            {
                "delete-services-step-input-5.json", null,
            },
            // (5) The user does not have the necessary rights to delete the service:
            {
                "delete-services-step-input-6.json", MessageFormat.format(Messages.ERROR_DELETING_SERVICE, "service-2", "servicelabel", "serviceplan", "Controller operation failed: 403 Forbidden")},
        // @formatter:on
        });
    }

    public DeleteServicesStepTest(String stepInput, String expectedExceptionMessage) throws Exception {
        System.out.println(stepInput);
        this.stepInput = JsonUtil.fromJson(TestUtil.getResourceAsString(stepInput, DeleteServicesStepTest.class), StepInput.class);
        this.expectedExceptionMessage = expectedExceptionMessage;
    }

    @Before
    public void setUp() throws Exception {
        loadParameters();
        prepareContext();
        prepareClient();
    }

    @Test
    public void testExecute() throws Exception {
        prepareResponses(STEP_EXECUTION);
        step.execute(context);
        assertStepPhase(STEP_EXECUTION);
        verifyClient();

        prepareResponses(POLLING);
        step.execute(context);
        assertStepPhase(POLLING);
        verifyClient();
    }

    @SuppressWarnings("unchecked")
    private void assertStepPhase(String stepPhase) {
        Map<String, Object> stepPhaseResults = (Map<String, Object>) stepInput.stepPhaseResults.get(stepPhase);
        String expectedStepPhase = (String) stepPhaseResults.get("expextedStepPhase");
        assertEquals(expectedStepPhase, getExecutionStatus());
    }

    private void loadParameters() {
        servicesToDelete = stepInput.servicesToDelete.stream()
            .map((service) -> service.name)
            .collect(Collectors.toList());
        servicesGuids = stepInput.servicesToDelete.stream()
            .collect(Collectors.toMap(e -> e.name, e -> e.guid));

        if (expectedExceptionMessage != null) {
            expectedException.expect(SLException.class);
            expectedException.expectMessage(expectedExceptionMessage);
        }
    }

    private void prepareContext() {
        StepsUtil.setArrayVariableFromCollection(context, Constants.VAR_SERVICES_TO_DELETE, servicesToDelete);
        StepsUtil.setAsBinaryJson(context, Constants.VAR_SERVICES_GUIDS, servicesGuids);
        context.setVariable(com.sap.cloud.lm.sl.cf.process.Constants.PARAM_DELETE_SERVICES, true);
        context.setVariable(com.sap.cloud.lm.sl.cf.persistence.message.Constants.VARIABLE_NAME_SPACE_ID, TEST_SPACE_ID);
        when(clientProvider.getControllerClient(anyString(), anyString())).thenReturn(client);
        when(clientProvider.getControllerClient(anyString(), anyString(), anyString(), anyString())).thenReturn(client);
    }

    private void prepareClient() {
        for (SimpleService service : stepInput.servicesToDelete) {
            Mockito.when(client.getServiceInstance(service.name))
                .thenReturn(createServiceInstance(service));
            if (service.httpErrorCodeToReturnOnDelete != null) {
                HttpStatus httpStatusToReturnOnDelete = HttpStatus.valueOf(service.httpErrorCodeToReturnOnDelete);
                Mockito.doThrow(new CloudOperationException(httpStatusToReturnOnDelete))
                    .when(client)
                    .deleteService(service.name);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void prepareResponses(String stepPhase) {
        Map<String, Object> stepPhaseResults = (Map<String, Object>) stepInput.stepPhaseResults.get(stepPhase);

        prepareServiceInstanceGetter(stepPhaseResults);
        prepareEventsGetter(stepPhaseResults);
    }

    private void prepareEventsGetter(Map<String, Object> stepPhaseResponse) {
        Mockito.reset(eventsGetter);
        Map<String, Map<String, Boolean>> eventsResponse = (Map<String, Map<String, Boolean>>) stepPhaseResponse.get("eventsResponse");
        stepInput.servicesToDelete.stream()
            .filter(service -> eventsResponse.get(service.name)
                .containsKey("containsDeleteEvent"))
            .filter(service -> eventsResponse.get(service.name)
                .get("containsDeleteEvent")
                .equals(true))
            .forEach(service -> {
                CloudEvent deleteEvent = createDeleteServiceCloudEvent(service);
                List<CloudEvent> events = createOlderServiceCloudEvents(deleteEvent, 5);
                events.add(deleteEvent);

                Mockito.when(eventsGetter.getEvents(UUID.fromString(service.guid), client))
                    .thenReturn(events);
                Mockito.when(eventsGetter.getLastEvent(UUID.fromString(service.guid), client))
                    .thenReturn(deleteEvent);
            });
        Mockito.when(eventsGetter.isDeleteEvent(SERVICE_EVENT_TYPE_DELETE)).thenCallRealMethod();
    }

    @SuppressWarnings("unchecked")
    private void prepareServiceInstanceGetter(Map<String, Object> stepPhaseResponse) {
        Mockito.reset(serviceGetter);
        Map<String, Object> serviceInstanceResponse = (Map<String, Object>) stepPhaseResponse.get("serviceInstanceResponse");
        for (Entry<String, Object> entry : serviceInstanceResponse.entrySet()) {
            String serviceName = entry.getKey();
            Map<String, Object> response = (Map<String, Object>) entry.getValue();
            Map<String, Object> entryResponse = (Map<String, Object>) response.get("entity");

            Mockito.when(serviceGetter.getServiceInstanceEntity(client, serviceName, TEST_SPACE_ID))
                .thenReturn(entryResponse);
            Mockito.when(serviceGetter.getServiceInstance(client, serviceName, TEST_SPACE_ID))
                .thenReturn(response);
        }
    }

    private CloudServiceInstance createServiceInstance(SimpleService service) {
        CloudServiceInstance instance = new CloudServiceInstance();
        if (service.hasBoundApplications) {
            instance.setBindings(Arrays.asList(new CloudServiceBinding()));
        }
        CloudService cloudService = new CloudService();
        cloudService.setName(service.name);
        cloudService.setLabel(service.label);
        cloudService.setPlan(service.plan);
        UUID guid = UUID.fromString(service.guid);
        cloudService.setMeta(new Meta(guid, null, null));
        instance.setService(cloudService);
        return instance;
    }

    private CloudEvent createDeleteServiceCloudEvent(SimpleService service) {
        CloudEvent event = new CloudEvent(null, null);
        event.setActee(service.guid);
        event.setActeeName(service.name);
        event.setType(SERVICE_EVENT_TYPE_DELETE);
        event.setTimestamp(new Date());
        return event;
    }

    private List<CloudEvent> createOlderServiceCloudEvents(CloudEvent lastEvent, int count) {
        Assert.assertTrue(count > 0);
        List<CloudEvent> events = new ArrayList<>();
        CloudEvent currentEvent = lastEvent;
        for (int i = 0; i < count; i++) {
            CloudEvent event = new CloudEvent(null, null);
            event.setActee(lastEvent.getActee());
            event.setActeeName(lastEvent.getActeeName());
            event.setType("nonDelete");

            Date olderDate = getOlderDateFromEvent(currentEvent);

            event.setTimestamp(olderDate);
            events.add(event);
            currentEvent = event;
        }
        return events;
    }

    private Date getOlderDateFromEvent(CloudEvent lastEvent) {
        ZoneId systemDefaultZone = ZoneId.systemDefault();
        Instant lastEventInstant = lastEvent.getTimestamp()
            .toInstant();
        LocalDateTime lastEventDateTime = LocalDateTime.ofInstant(lastEventInstant, systemDefaultZone);
        LocalDateTime secondBefore = lastEventDateTime.minusSeconds(1);
        Instant secondBeforeInstant = secondBefore.atZone(systemDefaultZone)
            .toInstant();
        return Date.from(secondBeforeInstant);
    }

    private void verifyClient() {
        for (SimpleService service : stepInput.servicesToDelete) {
            if (!service.hasBoundApplications) {
                Mockito.verify(client, Mockito.times(1))
                    .deleteService(service.name);
            }
        }
    }

    private static class StepInput {
        List<SimpleService> servicesToDelete;
        Map<String, Object> stepPhaseResults;
    }

    private static class SimpleService {
        String name;
        String label;
        String plan;
        String guid;
        boolean hasBoundApplications;
        Integer httpErrorCodeToReturnOnDelete;
    }

    @Override
    protected DeleteServicesStep createStep() {
        return new DeleteServicesStep();
    }

}

package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.CloudMetadata;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.cloudfoundry.client.lib.domain.ImmutableCloudApplication;
import org.cloudfoundry.client.lib.domain.ImmutableCloudEvent;
import org.cloudfoundry.client.lib.domain.ImmutableCloudEvent.ImmutableParticipant;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceBinding;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceKey;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceExtended;
import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.EventsGetter;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceGetter;
import com.sap.cloud.lm.sl.cf.process.message.Messages;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationGetter;
import com.sap.cloud.lm.sl.cf.process.util.ServiceProgressReporter;
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
    private List<CloudServiceExtended> servicesData = new ArrayList<>();

    private CloudMetadata metadata = ImmutableCloudMetadata.builder()
                                                           .guid(UUID.randomUUID())
                                                           .build();

    @Mock
    protected CloudControllerClient client;

    @Mock
    private ServiceProgressReporter serviceProgressReporter;
    @Mock
    private ServiceGetter serviceGetter;
    @Mock
    private EventsGetter eventsGetter;
    @InjectMocks
    private ServiceOperationGetter serviceOperationGetter;

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
            // (1) Services do not have bindings, but have service keys:
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
                "delete-services-step-input-6.json", MessageFormat.format(Messages.ERROR_DELETING_SERVICE, "service-2", "servicelabel", "serviceplan", "Controller operation failed: 403 Forbidden")
            },
            // (6) One of the services does not have bindings, but has service keys; The other one has bindings, but does not have service keys:
            {
                "delete-services-step-input-7.json", null,
            }
        // @formatter:on
        });
    }

    public DeleteServicesStepTest(String stepInput, String expectedExceptionMessage) throws Exception {
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
        if (StepsUtil.getServicesToDelete(context)
                     .isEmpty()) {
            return;
        }
        prepareResponses(STEP_EXECUTION);
        step.execute(context);
        assertStepPhase(STEP_EXECUTION);
        verifyClient();

        prepareResponses(POLLING);
        step.execute(context);
        assertStepPhase(POLLING);
        verifyClient();
        verifyServiceUnbinding();
        verifyServiceKeyDeletion();
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
        servicesData = stepInput.servicesToDelete.stream()
                                                 .map(this::toCloudServiceExtended)
                                                 .collect(Collectors.toList());

        if (expectedExceptionMessage != null) {
            expectedException.expect(SLException.class);
            expectedException.expectMessage(expectedExceptionMessage);
        }
    }

    private CloudServiceExtended toCloudServiceExtended(SimpleService simpleService) {
        return ImmutableCloudServiceExtended.builder()
                                            .metadata(ImmutableCloudMetadata.builder()
                                                                            .guid(UUID.fromString(simpleService.guid))
                                                                            .build())
                                            .name(simpleService.name)
                                            .build();
    }

    private void prepareContext() {
        StepsUtil.setServicesToDelete(context, servicesToDelete);
        StepsUtil.setServicesData(context, servicesData);
        context.setVariable(com.sap.cloud.lm.sl.cf.process.Constants.PARAM_DELETE_SERVICES, true);
        context.setVariable(com.sap.cloud.lm.sl.cf.persistence.Constants.VARIABLE_NAME_SPACE_ID, TEST_SPACE_ID);
        when(clientProvider.getControllerClient(any(), any())).thenReturn(client);
        when(clientProvider.getControllerClient(any(), any(), any(), any())).thenReturn(client);
    }

    private void prepareClient() {
        for (SimpleService service : stepInput.servicesToDelete) {
            Mockito.when(client.getService(ArgumentMatchers.eq(service.name), ArgumentMatchers.anyBoolean()))
                   .thenReturn(createCloudService(service));
            Mockito.when(client.getService(service.name))
                   .thenReturn(createCloudService(service));
            Mockito.when(client.getServiceInstance(service.name))
                   .thenReturn(createCloudServiceInstance(service));
            if (service.hasBoundApplications) {
                Mockito.when(client.getApplications())
                       .thenReturn(Arrays.asList(createCloudApplication(metadata)));
            }
            if (service.hasServiceKeys) {
                Mockito.when(client.getServiceKeys(service.name))
                       .thenReturn(Arrays.asList(createCloudServiceKey(metadata)));
            }
            if (service.httpErrorCodeToReturnOnDelete != null) {
                HttpStatus httpStatusToReturnOnDelete = HttpStatus.valueOf(service.httpErrorCodeToReturnOnDelete);
                Mockito.doThrow(new CloudOperationException(httpStatusToReturnOnDelete))
                       .when(client)
                       .deleteService(service.name);
            }
        }
    }

    private CloudApplication createCloudApplication(CloudMetadata metadata) {
        return ImmutableCloudApplication.builder()
                                        .metadata(metadata)
                                        .build();
    }

    private CloudServiceKey createCloudServiceKey(CloudMetadata metadata) {
        return ImmutableCloudServiceKey.builder()
                                       .metadata(metadata)
                                       .build();
    }

    private CloudService createCloudService(SimpleService service) {
        return ImmutableCloudServiceExtended.builder()
                                            .metadata(ImmutableCloudMetadata.builder()
                                                                            .guid(UUID.fromString(service.guid))
                                                                            .build())
                                            .name(service.name)
                                            .plan(service.plan)
                                            .label(service.label)
                                            .build();
    }

    @SuppressWarnings("unchecked")
    private void prepareResponses(String stepPhase) {
        Map<String, Object> stepPhaseResults = (Map<String, Object>) stepInput.stepPhaseResults.get(stepPhase);

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
                                      Collections.shuffle(events);

                                      Mockito.when(eventsGetter.getEvents(UUID.fromString(service.guid), client))
                                             .thenReturn(events);
                                      Mockito.when(eventsGetter.getLastEvent(UUID.fromString(service.guid), client))
                                             .thenReturn(deleteEvent);
                                  });
        Mockito.when(eventsGetter.isDeleteEvent(SERVICE_EVENT_TYPE_DELETE))
               .thenCallRealMethod();
    }

    private CloudServiceInstance createCloudServiceInstance(SimpleService service) {
        return ImmutableCloudServiceInstance.builder()
                                            .bindings(createCloudServiceBindings(service))
                                            .service(createCloudService(service))
                                            .build();
    }

    private List<CloudServiceBinding> createCloudServiceBindings(SimpleService service) {
        if (!service.hasBoundApplications) {
            return Collections.emptyList();
        }
        return Arrays.asList(ImmutableCloudServiceBinding.builder()
                                                         .applicationGuid(metadata.getGuid())
                                                         .build());
    }

    private CloudEvent createDeleteServiceCloudEvent(SimpleService service) {
        return ImmutableCloudEvent.builder()
                                  .actee(ImmutableParticipant.builder()
                                                             .guid(UUID.fromString(service.guid))
                                                             .name(service.name)
                                                             .build())
                                  .type(SERVICE_EVENT_TYPE_DELETE)
                                  .timestamp(new Date())
                                  .build();
    }

    private List<CloudEvent> createOlderServiceCloudEvents(CloudEvent lastEvent, int count) {
        Assert.assertTrue(count > 0);
        List<CloudEvent> events = new ArrayList<>();
        CloudEvent currentEvent = lastEvent;
        for (int i = 0; i < count; i++) {
            CloudEvent event = ImmutableCloudEvent.builder()
                                                  .actee(lastEvent.getActee())
                                                  .type("nonDelete")
                                                  .timestamp(getOlderDateFromEvent(currentEvent))
                                                  .build();
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

    private void verifyServiceUnbinding() {
        for (SimpleService service : stepInput.servicesToDelete) {
            if (service.hasBoundApplications) {
                Mockito.verify(client, Mockito.atLeastOnce())
                       .unbindService(any(), Mockito.eq(service.name));
            }
        }
    }

    private void verifyServiceKeyDeletion() {
        for (SimpleService service : stepInput.servicesToDelete) {
            if (service.hasServiceKeys) {
                Mockito.verify(client, Mockito.atLeastOnce())
                       .deleteServiceKey(Mockito.eq(service.name), any());
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
        boolean hasServiceKeys;
        Integer httpErrorCodeToReturnOnDelete;
    }

    @Override
    protected DeleteServicesStep createStep() {
        serviceOperationGetter = new ServiceOperationGetter(serviceGetter, eventsGetter);
        return new DeleteServicesStep(serviceOperationGetter, serviceProgressReporter);
    }

}

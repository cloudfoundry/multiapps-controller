package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.cloudfoundry.client.lib.domain.ImmutableCloudEvent;
import org.cloudfoundry.client.lib.domain.ImmutableCloudEvent.ImmutableParticipant;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceBinding;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceKey;
import org.cloudfoundry.client.lib.domain.ServiceOperation;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.cf.clients.EventsGetter;
import org.cloudfoundry.multiapps.controller.core.cf.clients.ServiceGetter;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceAction;
import org.cloudfoundry.multiapps.controller.process.util.ServiceOperationGetter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceProgressReporter;
import org.cloudfoundry.multiapps.controller.process.util.ServiceRemover;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

class DeleteServiceStepTest extends SyncFlowableStepTest<DeleteServiceStep> {

    private static final String SEVICE_KEY_NAME = "test-service-key";
    private static final String SERVICE_NAME = "test-service";
    private static final String SERVICE_GUID = "5ee63aa7-fb56-4e8f-b43f-a74efead2602";
    private static final String SERVICE_EVENT_TYPE_DELETE = "audit.service_instance.delete";

    @Mock
    private ServiceProgressReporter serviceProgressReporter;
    @Mock
    private ServiceGetter serviceGetter;
    @Mock
    private EventsGetter eventsGetter;
    @Mock
    private ServiceRemover serviceRemover;
    @InjectMocks
    private ServiceOperationGetter serviceOperationGetter;

    static Stream<Arguments> testServiceDelete() {
        return Stream.of(Arguments.of(true, false, true, true, StepPhase.DONE), Arguments.of(false, false, true, true, StepPhase.DONE),
                         Arguments.of(false, false, false, true, StepPhase.DONE), Arguments.of(false, false, false, false, StepPhase.POLL),
                         Arguments.of(true, false, false, true, StepPhase.DONE), Arguments.of(true, false, true, false, StepPhase.POLL),
                         Arguments.of(false, true, true, true, StepPhase.DONE), Arguments.of(false, true, false, true, StepPhase.POLL),
                         Arguments.of(false, true, false, false, StepPhase.POLL));
    }

    @ParameterizedTest
    @MethodSource
    void testServiceDelete(boolean shouldRecreateService, boolean shouldDeleteServiceKeys, boolean hasServiceBindings,
                           boolean hasServiceKeys, StepPhase expectedStepPhase) {
        prepareActionsToExecute(shouldRecreateService, shouldDeleteServiceKeys);
        UUID serviceGuid = UUID.fromString(SERVICE_GUID);
        prepareContext();
        CloudServiceInstance cloudServiceInstance = createCloudService(serviceGuid);
        List<CloudServiceKey> serviceKeys = createServiceKeys(cloudServiceInstance, hasServiceKeys);
        prepareClient(cloudServiceInstance, serviceKeys, hasServiceBindings);

        step.execute(context.getExecution());

        assertStepPhase(expectedStepPhase);
        assertServiceRemoverCall(expectedStepPhase);
    }

    private void prepareActionsToExecute(boolean shouldRecreateService, boolean shouldDeleteServiceKeys) {
        List<ServiceAction> actionsToExecute = new ArrayList<>();
        if (shouldRecreateService) {
            actionsToExecute.add(ServiceAction.RECREATE);
        }
        context.setVariable(Variables.SERVICE_ACTIONS_TO_EXCECUTE, actionsToExecute);
        context.setVariable(Variables.DELETE_SERVICE_KEYS, shouldDeleteServiceKeys);
    }

    @Test
    void testWithNullVariable() {
        step.execute(context.getExecution());

        verify(stepLogger).debug(Messages.MISSING_SERVICE_TO_DELETE);
        assertStepFinishedSuccessfully();
    }

    @Test
    void testServicePolling() {
        UUID serviceGuid = UUID.fromString(SERVICE_GUID);
        prepareContext();
        CloudServiceInstance serviceInstance = createCloudService(serviceGuid);
        prepareClient(serviceInstance, Collections.emptyList(), false);

        prepareEventsGetter(false, serviceGuid);
        step.execute(context.getExecution());
        assertStepPhase(StepPhase.POLL);

        prepareEventsGetter(true, serviceGuid);
        step.execute(context.getExecution());
        assertStepPhase(StepPhase.DONE);
    }

    private void prepareContext() {
        context.setVariable(Variables.SERVICE_TO_DELETE, SERVICE_NAME);
        context.setVariable(Variables.DELETE_SERVICES, true);
    }

    private CloudServiceInstance createCloudService(UUID serviceGuid) {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .metadata(ImmutableCloudMetadata.builder()
                                                                                    .guid(serviceGuid)
                                                                                    .build())
                                                    .name(SERVICE_NAME)
                                                    .build();
    }

    private void prepareClient(CloudServiceInstance serviceInstance, List<CloudServiceKey> serviceKeys, boolean hasServiceBindings) {
        when(client.getServiceInstance(eq(SERVICE_NAME), anyBoolean())).thenReturn(serviceInstance);
        when(client.getServiceKeys(serviceInstance)).thenReturn(serviceKeys);
        if (hasServiceBindings) {
            when(client.getServiceBindings(serviceInstance.getMetadata()
                                                          .getGuid())).thenReturn(Collections.singletonList(createServiceBinding()));
        }
    }

    private CloudServiceBinding createServiceBinding() {
        return ImmutableCloudServiceBinding.builder()
                                           .applicationGuid(UUID.randomUUID())
                                           .build();
    }

    private List<CloudServiceKey> createServiceKeys(CloudServiceInstance serviceInstance, boolean hasServiceKeys) {
        if (hasServiceKeys) {
            return Collections.singletonList(ImmutableCloudServiceKey.builder()
                                                                     .name(SEVICE_KEY_NAME)
                                                                     .serviceInstance(serviceInstance)
                                                                     .build());
        }
        return Collections.emptyList();
    }

    private void prepareEventsGetter(boolean containsDeleteEvent, UUID serviceGuid) {
        reset();
        if (containsDeleteEvent) {
            CloudEvent deleteEvent = createDeleteServiceCloudEvent(serviceGuid);
            List<CloudEvent> events = createOlderServiceCloudEvents(deleteEvent, 5);
            events.add(deleteEvent);
            Collections.shuffle(events);

            Mockito.when(eventsGetter.getEvents(eq(serviceGuid), any(CloudControllerClient.class)))
                   .thenReturn(events);
            Mockito.when(eventsGetter.getLastEvent(eq(serviceGuid), any(CloudControllerClient.class)))
                   .thenReturn(deleteEvent);

        }

        Mockito.when(eventsGetter.isDeleteEvent(SERVICE_EVENT_TYPE_DELETE))
               .thenCallRealMethod();
    }

    private CloudEvent createDeleteServiceCloudEvent(UUID serviceGuid) {
        return ImmutableCloudEvent.builder()
                                  .actee(ImmutableParticipant.builder()
                                                             .guid(serviceGuid)
                                                             .name(SERVICE_NAME)
                                                             .build())
                                  .type(SERVICE_EVENT_TYPE_DELETE)
                                  .timestamp(new Date())
                                  .build();
    }

    private List<CloudEvent> createOlderServiceCloudEvents(CloudEvent lastEvent, int count) {
        assertTrue(count > 0);
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

    private void assertStepPhase(StepPhase expectedStepPhase) {
        assertEquals(expectedStepPhase.toString(), getExecutionStatus());
    }

    private void assertServiceRemoverCall(StepPhase expectedStepPhase) {
        int callTimes = expectedStepPhase.equals(StepPhase.DONE) ? 0 : 1;
        verify(serviceRemover, times(callTimes)).deleteService(any(), any(), anyList(), anyList());
    }

    @Override
    protected DeleteServiceStep createStep() {
        serviceOperationGetter = new ServiceOperationGetter(serviceGetter, eventsGetter);
        return new DeleteServiceStep(serviceOperationGetter, serviceProgressReporter, serviceRemover);
    }

}

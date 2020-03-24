package com.sap.cloud.lm.sl.cf.process.steps;

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
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.cloudfoundry.client.lib.domain.ImmutableCloudEvent;
import org.cloudfoundry.client.lib.domain.ImmutableCloudEvent.ImmutableParticipant;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceBinding;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import com.sap.cloud.lm.sl.cf.client.lib.domain.ImmutableCloudServiceExtended;
import com.sap.cloud.lm.sl.cf.core.cf.clients.EventsGetter;
import com.sap.cloud.lm.sl.cf.core.cf.clients.ServiceGetter;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.Messages;
import com.sap.cloud.lm.sl.cf.process.analytics.model.ServiceAction;
import com.sap.cloud.lm.sl.cf.process.util.ServiceOperationGetter;
import com.sap.cloud.lm.sl.cf.process.util.ServiceProgressReporter;
import com.sap.cloud.lm.sl.cf.process.util.ServiceRemover;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

public class DeleteServiceStepTest extends SyncFlowableStepTest<DeleteServiceStep> {

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
        return Stream.of(
        // @formatter:off
                        Arguments.of(true, false, true, true, StepPhase.DONE),
                        Arguments.of(false, false, true, true, StepPhase.DONE),
                        Arguments.of(false, false, false, true, StepPhase.DONE),
                        Arguments.of(false, false, false, false, StepPhase.POLL),
                        Arguments.of(true, false, false, true, StepPhase.DONE),
                        Arguments.of(true, false, true, false, StepPhase.POLL),
                        Arguments.of(false, true, true, true, StepPhase.DONE),
                        Arguments.of(false, true, false, true, StepPhase.POLL),
                        Arguments.of(false, true, false, false, StepPhase.POLL)
        // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testServiceDelete(boolean shouldRecreateService, boolean shouldDeleteServiceKeys, boolean hasServiceBindings,
                                  boolean hasServiceKeys, StepPhase expectedStepPhase) {
        prepareActionsToExecute(shouldRecreateService, shouldDeleteServiceKeys);
        UUID serviceGuid = UUID.fromString(SERVICE_GUID);
        prepareContext();
        CloudServiceInstance cloudServiceInstance = createCloudServiceInstance(serviceGuid, hasServiceBindings);
        List<CloudServiceKey> serviceKeys = createServiceKeys(cloudServiceInstance.getService(), hasServiceKeys);
        prepareClient(cloudServiceInstance, serviceKeys);

        step.execute(context.getExecution());

        assertStepPhase(expectedStepPhase);
        assertServiceRemoverCall(expectedStepPhase);
    }

    private void prepareActionsToExecute(boolean shouldRecreateService, boolean shouldDeleteServiceKeys) {
        List<ServiceAction> actionsToExecute = new ArrayList<>();
        if (shouldRecreateService) {
            actionsToExecute.add(ServiceAction.RECREATE);
        }
        StepsUtil.setServiceActionsToExecute(actionsToExecute, context.getExecution());
        context.getExecution()
               .setVariable(Constants.PARAM_DELETE_SERVICE_KEYS, shouldDeleteServiceKeys);
    }

    @Test
    public void testWithNullVariable() {
        step.execute(context.getExecution());

        verify(stepLogger).debug(Messages.MISSING_SERVICE_TO_DELETE);
        assertStepFinishedSuccessfully();
    }

    @Test
    public void testServicePolling() {
        UUID serviceGuid = UUID.fromString(SERVICE_GUID);
        prepareContext();
        CloudServiceInstance cloudServiceInstance = createCloudServiceInstance(serviceGuid, false);
        prepareClient(cloudServiceInstance, Collections.emptyList());

        prepareEventsGetter(false, serviceGuid);
        step.execute(context.getExecution());
        assertStepPhase(StepPhase.POLL);

        prepareEventsGetter(true, serviceGuid);
        step.execute(context.getExecution());
        assertStepPhase(StepPhase.DONE);
    }

    private void prepareContext() {
        context.setVariable(Variables.SERVICE_TO_DELETE, SERVICE_NAME);
        context.getExecution()
               .setVariable(com.sap.cloud.lm.sl.cf.process.Constants.PARAM_DELETE_SERVICES, true);
    }

    private CloudServiceInstance createCloudServiceInstance(UUID serviceGuid, boolean hasServiceBindings) {
        ImmutableCloudServiceInstance.Builder cloudServiceInstance = ImmutableCloudServiceInstance.builder()
                                                                                                  .service(createCloudService(serviceGuid));
        if (hasServiceBindings) {
            cloudServiceInstance.addBinding(createServiceBinding());
        }

        return cloudServiceInstance.build();
    }

    private CloudServiceBinding createServiceBinding() {
        return ImmutableCloudServiceBinding.builder()
                                           .applicationGuid(UUID.randomUUID())
                                           .build();
    }

    private CloudService createCloudService(UUID serviceGuid) {
        return ImmutableCloudServiceExtended.builder()
                                            .metadata(ImmutableCloudMetadata.builder()
                                                                            .guid(serviceGuid)
                                                                            .build())
                                            .name(SERVICE_NAME)
                                            .build();
    }

    private void prepareClient(CloudServiceInstance cloudServiceInstance, List<CloudServiceKey> serviceKeys) {
        when(client.getServiceInstance(eq(SERVICE_NAME), anyBoolean())).thenReturn(cloudServiceInstance);
        when(client.getServiceKeys(eq(cloudServiceInstance.getService()))).thenReturn(serviceKeys);
    }

    private List<CloudServiceKey> createServiceKeys(CloudService service, boolean hasServiceKeys) {
        if (hasServiceKeys) {
            return Collections.singletonList(ImmutableCloudServiceKey.builder()
                                                                     .name(SEVICE_KEY_NAME)
                                                                     .service(service)
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
        verify(serviceRemover, times(callTimes)).deleteService(any(), any(), anyList());
    }

    @Override
    protected DeleteServiceStep createStep() {
        serviceOperationGetter = new ServiceOperationGetter(serviceGetter, eventsGetter);
        return new DeleteServiceStep(serviceOperationGetter, serviceProgressReporter, serviceRemover);
    }

}

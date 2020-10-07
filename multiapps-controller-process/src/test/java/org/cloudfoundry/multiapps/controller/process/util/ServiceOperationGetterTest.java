package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.ImmutableCloudEvent;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ServiceOperation;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ServiceOperationGetterTest {

    private static final String SERVICE_NAME = "service";
    private static final UUID SERVICE_GUID = UUID.randomUUID();

    @Mock
    private ProcessContext context;
    @Mock
    private CloudServiceInstanceExtended service;
    @Mock
    private CloudControllerClient client;

    private ServiceOperationGetter serviceOperationGetter;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        serviceOperationGetter = new ServiceOperationGetter();
    }

    static Stream<Arguments> testGetLastServiceOperation() {
        return Stream.of(
            // (1) Test with create succeeded operation
            Arguments.of(ServiceOperation.Type.CREATE, ServiceOperation.State.SUCCEEDED, "created",
                         new ServiceOperation(ServiceOperation.Type.CREATE, "created", ServiceOperation.State.SUCCEEDED)),
            // (2) Test with delete service in progress operation
            Arguments.of(ServiceOperation.Type.DELETE, ServiceOperation.State.IN_PROGRESS, null,
                         new ServiceOperation(ServiceOperation.Type.DELETE, null, ServiceOperation.State.IN_PROGRESS)),
            // (3) Test with missing service entity
            Arguments.of(null, null, null, null));
    }

    @ParameterizedTest
    @MethodSource
    void testGetLastServiceOperation(ServiceOperation.Type serviceOperationType, ServiceOperation.State serviceOperationState,
                                     String description, ServiceOperation expectedServiceOperation) {
        when(service.getName()).thenReturn(SERVICE_NAME);
        if (serviceOperationState != null && serviceOperationType != null) {
            when(service.getLastOperation()).thenReturn(new ServiceOperation(serviceOperationType, description, serviceOperationState));
        }
        when(client.getServiceInstance(SERVICE_NAME, false)).thenReturn(service);
        when(context.getControllerClient()).thenReturn(client);

        ServiceOperation serviceOperation = serviceOperationGetter.getLastServiceOperation(context, service);

        assertServiceOperation(expectedServiceOperation, serviceOperation);
    }

    private void assertServiceOperation(ServiceOperation expectedServiceOperation, ServiceOperation serviceOperation) {
        if (expectedServiceOperation != null) {
            assertEquals(expectedServiceOperation.getState(), serviceOperation.getState());
            assertEquals(expectedServiceOperation.getType(), serviceOperation.getType());
            assertEquals(expectedServiceOperation.getDescription(), serviceOperation.getDescription());
        } else {
            assertNull(serviceOperation);
        }
    }

    static Stream<Arguments> testGetLastDeleteServiceOperation() {
        return Stream.of(
            //(1) test with null metadata returns null service operation
            Arguments.of(true, false, null),
            //(2) test with delete event returns succeeded operation
            Arguments.of(false, true, new ServiceOperation(ServiceOperation.Type.DELETE, null, ServiceOperation.State.SUCCEEDED)),
            //(3) test with non-delete event returns in progress operation
            Arguments.of(false, false, new ServiceOperation(ServiceOperation.Type.DELETE, null, ServiceOperation.State.IN_PROGRESS)));
    }

    @ParameterizedTest
    @MethodSource
    void testGetLastDeleteServiceOperation(boolean missingServiceMetadata, boolean containsDeleteEvent,
                                           ServiceOperation expectedOperation) {
        prepareService(missingServiceMetadata);
        prepareEvents(containsDeleteEvent);
        when(context.getControllerClient()).thenReturn(client);

        ServiceOperation serviceOperation = serviceOperationGetter.getLastServiceOperation(context, service);

        assertEquals(expectedOperation, serviceOperation);
    }

    private void prepareService(boolean isMissingServiceMetadata) {
        if (!isMissingServiceMetadata) {
            when(service.getMetadata()).thenReturn(ImmutableCloudMetadata.of(SERVICE_GUID));
        }
    }

    private void prepareEvents(boolean containsDeleteEvent) {
        CloudEvent event;
        if (containsDeleteEvent) {
            event = ImmutableCloudEvent.builder()
                                       .type("audit.service_instance.delete")
                                       .build();
        } else {
            event = ImmutableCloudEvent.builder()
                                       .type("audit.service_instance.create")
                                       .build();
        }
        when(client.getEventsByActee(SERVICE_GUID)).thenReturn(List.of(event));
    }

}

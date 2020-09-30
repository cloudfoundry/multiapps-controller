package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.CloudControllerClient;
import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.CloudServiceBrokerException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.cloudfoundry.client.lib.domain.ImmutableCloudApplication;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceBinding;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceKey;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

class ServiceRemoverTest {

    private static final String TEST_USER = "test-user";
    private static final String TEST_SPACE = "test-space";
    private static final String SERVICE_NAME = "test-service";
    private static final String SERVICE_LABEL = "test-label";
    private static final String SERVICE_PLAN = "test-service-plan";
    private static final String BINDING_NAME = "test-binding-1";
    private static final String SERVICE_KEY_NAME = "test-service-key";

    @Mock
    private ApplicationConfiguration configuration;
    @Mock
    private StepLogger stepLogger;
    @Mock
    private CloudControllerClient client;
    @Mock
    private CloudControllerClientProvider clientProvider;

    private final DelegateExecution execution = MockDelegateExecution.createSpyInstance();

    private ProcessContext context;
    private ServiceRemover serviceRemover;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        prepareExecution();
        serviceRemover = new ServiceRemover(configuration);
    }

    private void prepareExecution() {
        when(clientProvider.getControllerClient(anyString(), anyString())).thenReturn(client);
        context = new ProcessContext(execution, stepLogger, clientProvider);
        context.setVariable(Variables.USER, TEST_USER);
        execution.setVariable(org.cloudfoundry.multiapps.controller.persistence.Constants.VARIABLE_NAME_SPACE_ID, TEST_SPACE);
    }

    static Stream<Arguments> testControllerErrorHandling() {
        return Stream.of(Arguments.of(HttpStatus.NOT_FOUND, null, null),
                         Arguments.of(HttpStatus.BAD_GATEWAY, CloudServiceBrokerException.class,
                                      "Service broker operation failed: 502 Bad Gateway"),
                         Arguments.of(HttpStatus.UNPROCESSABLE_ENTITY, CloudControllerException.class,
                                      "Controller operation failed: 422 Unprocessable Entity"),
                         Arguments.of(HttpStatus.FORBIDDEN, CloudControllerException.class, "Controller operation failed: 403 Forbidden"));
    }

    @ParameterizedTest
    @MethodSource
    void testControllerErrorHandling(HttpStatus httpStatusToThrow, Class<? extends Exception> expectedExceptionType,
                                     String expectedExceptionMessage) {
        UUID applicationBindingGuid = UUID.randomUUID();
        CloudServiceInstance serviceInstance = buildServiceInstance();

        CloudApplication application = buildApplication(applicationBindingGuid);
        CloudServiceBinding serviceBinding = buildServiceBinding(applicationBindingGuid);
        prepareClient(application, serviceInstance, serviceBinding);

        doThrow(new CloudOperationException(httpStatusToThrow)).when(client)
                                                               .deleteServiceInstance(any(CloudServiceInstance.class));

        if (expectedExceptionType == null) {
            assertDoesNotThrow(() -> serviceRemover.deleteService(context, serviceInstance, List.of(serviceBinding),
                                                                  Collections.emptyList()));
            return;
        }
        Exception wrappedException = assertThrows(SLException.class,
                                                  () -> serviceRemover.deleteService(context, serviceInstance, List.of(serviceBinding),
                                                                                     Collections.emptyList()));
        assertEquals(expectedExceptionType, wrappedException.getCause()
                                                            .getClass());
        assertEquals(MessageFormat.format(Messages.ERROR_DELETING_SERVICE, SERVICE_NAME, SERVICE_LABEL, SERVICE_PLAN,
                                          expectedExceptionMessage),
                     wrappedException.getMessage());
    }

    static Stream<Arguments> testDeleteServices() {
        return Stream.of(Arguments.of(false, true), Arguments.of(true, false), Arguments.of(false, false), Arguments.of(true, true));
    }

    @ParameterizedTest
    @MethodSource
    void testDeleteServices(boolean hasServiceBinding, boolean hasServiceKey) {
        UUID applicationBindingGuid = UUID.randomUUID();
        CloudServiceInstance serviceInstance = buildServiceInstance();
        List<CloudServiceKey> serviceKeys = buildServiceKeys(serviceInstance, hasServiceKey);

        CloudApplication application = buildApplication(applicationBindingGuid);
        CloudServiceBinding serviceBinding = null;
        if (hasServiceBinding) {
            serviceBinding = buildServiceBinding(applicationBindingGuid);
        }
        prepareClient(application, serviceInstance, serviceBinding);

        if (hasServiceBinding) {
            serviceRemover.deleteService(context, serviceInstance, List.of(serviceBinding), serviceKeys);
        } else {
            serviceRemover.deleteService(context, serviceInstance, Collections.emptyList(), serviceKeys);
        }

        assertClientOperations(hasServiceBinding, hasServiceKey);
    }

    private void assertClientOperations(boolean hasServiceBinding, boolean hasServiceKey) {
        int callingUnbindServiceCount = hasServiceBinding ? 1 : 0;
        verify(client, times(callingUnbindServiceCount)).unbindServiceInstance(any(CloudApplication.class),
                                                                               any(CloudServiceInstance.class));

        int callingDeleteServiceKeyCount = hasServiceKey ? 1 : 0;
        verify(client, times(callingDeleteServiceKeyCount)).deleteServiceKey(any(CloudServiceKey.class));
    }

    private CloudServiceInstance buildServiceInstance() {
        return ImmutableCloudServiceInstance.builder()
                                            .metadata(ImmutableCloudMetadata.builder()
                                                                            .guid(UUID.randomUUID())
                                                                            .build())
                                            .name(SERVICE_NAME)
                                            .label(SERVICE_LABEL)
                                            .plan(SERVICE_PLAN)
                                            .build();
    }

    private List<CloudServiceKey> buildServiceKeys(CloudServiceInstance service, boolean hasServiceKey) {
        if (hasServiceKey) {
            return List.of(ImmutableCloudServiceKey.builder()
                                                   .name(SERVICE_KEY_NAME)
                                                   .serviceInstance(service)
                                                   .build());
        }
        return Collections.emptyList();
    }

    private void prepareClient(CloudApplication application, CloudServiceInstance serviceInstance, CloudServiceBinding serviceBinding) {
        when(client.getApplication(application.getMetadata()
                                              .getGuid())).thenReturn(application);
        when(client.getServiceInstance(anyString())).thenReturn(serviceInstance);
        if (serviceBinding != null) {
            when(client.getServiceBindings(serviceInstance.getMetadata()
                                                          .getGuid())).thenReturn(List.of(serviceBinding));
        }
    }

    private CloudServiceBinding buildServiceBinding(UUID applicationBindingGuid) {
        return ImmutableCloudServiceBinding.builder()
                                           .name(BINDING_NAME)
                                           .applicationGuid(applicationBindingGuid)
                                           .build();
    }

    private CloudApplication buildApplication(UUID applicationBindingGuid) {
        return ImmutableCloudApplication.builder()
                                        .metadata(ImmutableCloudMetadata.builder()
                                                                        .guid(applicationBindingGuid)
                                                                        .build())
                                        .build();
    }

}

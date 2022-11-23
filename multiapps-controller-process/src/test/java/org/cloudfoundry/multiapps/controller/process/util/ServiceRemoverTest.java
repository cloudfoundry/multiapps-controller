package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.text.MessageFormat;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.cf.CloudControllerClientProvider;
import org.cloudfoundry.multiapps.controller.core.util.ApplicationConfiguration;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudControllerException;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.CloudServiceBrokerException;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudApplication;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceInstance;

class ServiceRemoverTest {

    private static final String TEST_USER = "test-user";
    private static final String TEST_SPACE = "test-space";
    private static final String SERVICE_NAME = "test-service";
    private static final String SERVICE_LABEL = "test-label";
    private static final String SERVICE_PLAN = "test-service-plan";

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
        when(clientProvider.getControllerClient(anyString(), anyString(), any())).thenReturn(client);
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
        prepareClient(application, serviceInstance);

        doThrow(new CloudOperationException(httpStatusToThrow)).when(client)
                                                               .deleteServiceInstance(any(CloudServiceInstance.class));

        if (expectedExceptionType == null) {
            assertDoesNotThrow(() -> serviceRemover.deleteService(context, serviceInstance));
            return;
        }
        Exception wrappedException = assertThrows(SLException.class, () -> serviceRemover.deleteService(context, serviceInstance));
        assertEquals(expectedExceptionType, wrappedException.getCause()
                                                            .getClass());
        assertEquals(MessageFormat.format(Messages.ERROR_DELETING_SERVICE, SERVICE_NAME, SERVICE_LABEL, SERVICE_PLAN,
                                          expectedExceptionMessage),
                     wrappedException.getMessage());
    }

    @Test
    void testDeleteService() {
        UUID applicationBindingGuid = UUID.randomUUID();
        CloudServiceInstance serviceInstance = buildServiceInstance();
        CloudApplication application = buildApplication(applicationBindingGuid);
        prepareClient(application, serviceInstance);
        serviceRemover.deleteService(context, serviceInstance);
        verify(client).deleteServiceInstance(serviceInstance);
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

    private void prepareClient(CloudApplication application, CloudServiceInstance serviceInstance) {
        when(client.getApplicationName(application.getGuid())).thenReturn(application.getName());
        when(client.getServiceInstance(anyString())).thenReturn(serviceInstance);
    }

    private CloudApplication buildApplication(UUID applicationBindingGuid) {
        return ImmutableCloudApplication.builder()
                                        .metadata(ImmutableCloudMetadata.of(applicationBindingGuid))
                                        .build();
    }

}

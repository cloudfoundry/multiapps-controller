package org.cloudfoundry.multiapps.controller.process.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import com.sap.cloudfoundry.client.facade.domain.ServiceCredentialBindingOperation;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudServiceInstanceExtended;
import org.cloudfoundry.multiapps.controller.core.helpers.MtaArchiveElements;
import org.cloudfoundry.multiapps.controller.persistence.services.FileService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.steps.ProcessContext;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ImmutableServiceCredentialBindingOperation;

class ServiceBindingParametersGetterTest {

    private static final String APP_NAME = "test_application";
    private static final String SERVICE_NAME = "test_service";
    private static final String APP_ARCHIVE_ID = "test_archive_id";
    private static final String SERVICE_BINDING_PARAMETERS_FILENAME = "test_binding_parameters.json";
    private static final UUID RANDOM_GUID = UUID.randomUUID();

    @Mock
    private ProcessContext context;
    @Mock
    private StepLogger stepLogger;
    @Mock
    private FileService fileService;
    @Mock
    private MtaArchiveElements mtaArchiveElements;
    @Mock
    private CloudControllerClient client;

    private ServiceBindingParametersGetter serviceBindingParametersGetter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        serviceBindingParametersGetter = new ServiceBindingParametersGetter(context, fileService, 0);
    }

    static Stream<Arguments> testGetServiceBindingParametersFromMta() {
        return Stream.of(
        //@formatter:off
                         Arguments.of(Map.of("param1", "value1"), Map.of("param2", "value2"), Map.of("param2", "value2", "param1", "value1")),
                         Arguments.of(Map.of("param1", "value1"), null, Map.of("param1", "value1")),
                         Arguments.of(null, Map.of("param2", "value2"), Map.of("param2", "value2")),
                         Arguments.of(null, null, Collections.emptyMap()),
                         Arguments.of(Map.of("object", Map.of("new-nested-parameter", "value1")),
                                Map.of("object", Map.of("file-nested-parameter", "value2")),
                                Map.of("object", Map.of("new-nested-parameter", "value1", "file-nested-parameter", "value2"))),
                        Arguments.of(Map.of("object", Map.of("nested", "value1")),
                                Map.of("object", Map.of("nested", "value2", "file-nested-parameter", "value2")),
                                Map.of("object", Map.of("nested", "value1", "file-nested-parameter", "value2")))
        //@formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGetServiceBindingParametersFromMta(Map<String, Object> descriptorParameters, Map<String, Object> filedProvidedParameters,
                                                Map<String, Object> expectedParameters)
        throws FileStorageException {
        CloudApplicationExtended application = buildApplication(descriptorParameters);
        CloudServiceInstanceExtended serviceInstance = buildServiceInstance();
        prepareMtaArchiveElements(filedProvidedParameters);
        prepareContext(serviceInstance);
        prepareFileService(filedProvidedParameters);

        Map<String, Object> bindingParameters = serviceBindingParametersGetter.getServiceBindingParametersFromMta(application,
                                                                                                                  SERVICE_NAME);

        assertEquals(expectedParameters, bindingParameters);
    }

    @Test
    void testGetServiceBindingOfMissingService() throws FileStorageException {
        CloudApplicationExtended application = buildApplication(null);
        when(context.getVariable(Variables.SERVICES_TO_BIND)).thenReturn(Collections.emptyList());

        Map<String, Object> bindingParameters = serviceBindingParametersGetter.getServiceBindingParametersFromMta(application,
                                                                                                                  SERVICE_NAME);

        assertTrue(bindingParameters.isEmpty(), "Binding parameters should be empty map");
    }

    @Test
    void testGetServiceBindingParametersFromExistingInstance() {
        CloudApplication application = buildApplication(null);
        CloudServiceInstanceExtended serviceInstance = buildServiceInstance();
        Map<String, Object> bindingParametersToReturn = Map.of("param1", "value1");
        prepareContext(serviceInstance);
        prepareClient(bindingParametersToReturn, true);

        Map<String, Object> bindingParameters = serviceBindingParametersGetter.getServiceBindingParametersFromExistingInstance(application,
                                                                                                                               SERVICE_NAME);

        assertEquals(bindingParametersToReturn, bindingParameters);
    }

    @Test
    void testThrowingExceptionWhenServiceBindingIsMissing() {
        CloudApplication application = buildApplication(null);
        CloudServiceInstanceExtended serviceInstance = buildServiceInstance();
        prepareContext(serviceInstance);
        prepareClient(null, false);

        assertThrows(CloudOperationException.class,
                     () -> serviceBindingParametersGetter.getServiceBindingParametersFromExistingInstance(application, SERVICE_NAME));
    }

    static Stream<Arguments> testHandleCloudOperationExceptions() {
        return Stream.of(Arguments.of(HttpStatus.BAD_REQUEST, false), Arguments.of(HttpStatus.NOT_FOUND, true),
                         Arguments.of(HttpStatus.NOT_IMPLEMENTED, false), Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, true));
    }

    @ParameterizedTest
    @MethodSource
    void testHandleCloudOperationExceptions(HttpStatus httpStatusToReturn, boolean shouldThrowException) {
        CloudApplication application = buildApplication(null);
        CloudServiceInstanceExtended serviceInstance = buildServiceInstance();
        prepareContext(serviceInstance);
        prepareClient(null, true);
        when(client.getServiceBindingParameters(RANDOM_GUID)).thenThrow(new CloudOperationException(httpStatusToReturn));

        if (shouldThrowException) {
            assertThrows(CloudOperationException.class,
                         () -> serviceBindingParametersGetter.getServiceBindingParametersFromExistingInstance(application, SERVICE_NAME));
            return;
        }
        Map<String, Object> bindingParameters = serviceBindingParametersGetter.getServiceBindingParametersFromExistingInstance(application,
                                                                                                                               SERVICE_NAME);
        assertNull(bindingParameters, "Returned result from CloudOperationException should be null");
        verify(stepLogger).warnWithoutProgressMessage(anyString(), any());
    }

    private CloudApplicationExtended buildApplication(Map<String, Object> descriptorParameters) {
        ImmutableCloudApplicationExtended.Builder applicationBuilder = ImmutableCloudApplicationExtended.builder()
                                                                                                        .name(APP_NAME)
                                                                                                        .moduleName(APP_NAME)
                                                                                                        .metadata(ImmutableCloudMetadata.builder()
                                                                                                                                        .guid(RANDOM_GUID)
                                                                                                                                        .build());
        if (descriptorParameters != null) {
            applicationBuilder.bindingParameters(Map.of(SERVICE_NAME, descriptorParameters));
        }
        return applicationBuilder.build();
    }

    private CloudServiceInstanceExtended buildServiceInstance() {
        return ImmutableCloudServiceInstanceExtended.builder()
                                                    .name(SERVICE_NAME)
                                                    .resourceName(SERVICE_NAME)
                                                    .metadata(ImmutableCloudMetadata.builder()
                                                                                    .guid(RANDOM_GUID)
                                                                                    .build())
                                                    .build();
    }

    private void prepareMtaArchiveElements(Map<String, Object> filedProvidedParameters) {
        if (filedProvidedParameters != null) {
            when(mtaArchiveElements.getRequiredDependencyFileName(anyString())).thenReturn(SERVICE_BINDING_PARAMETERS_FILENAME);
        }
    }

    private void prepareContext(CloudServiceInstanceExtended serviceInstance) {
        when(context.getStepLogger()).thenReturn(stepLogger);
        when(context.getVariable(Variables.SERVICES_TO_BIND)).thenReturn(Collections.singletonList(serviceInstance));
        when(context.getRequiredVariable(Variables.APP_ARCHIVE_ID)).thenReturn(APP_ARCHIVE_ID);
        when(context.getVariable(Variables.MTA_ARCHIVE_ELEMENTS)).thenReturn(mtaArchiveElements);
        when(context.getControllerClient()).thenReturn(client);
    }

    private void prepareFileService(Map<String, Object> filedProvidedParameters) throws FileStorageException {
        when(fileService.processFileContent(any(), any(), any())).thenReturn(filedProvidedParameters);
    }

    private void prepareClient(Map<String, Object> bindingParameters, boolean serviceBindingExist) {
        when(client.getRequiredServiceInstanceGuid(SERVICE_NAME)).thenReturn(RANDOM_GUID);
        when(client.getServiceBindingParameters(RANDOM_GUID)).thenReturn(bindingParameters);
        if (serviceBindingExist) {
            CloudServiceBinding serviceBinding = ImmutableCloudServiceBinding.builder()
                                                                             .applicationGuid(RANDOM_GUID)
                                                                             .serviceInstanceGuid(RANDOM_GUID)
                                                                             .metadata(ImmutableCloudMetadata.builder()
                                                                                                             .guid(RANDOM_GUID)
                                                                                                             .build())
                                                                             .serviceBindingOperation(ImmutableServiceCredentialBindingOperation.builder()
                                                                                                                                      .type(ServiceCredentialBindingOperation.Type.CREATE)
                                                                                                                                      .state(ServiceCredentialBindingOperation.State.SUCCEEDED)
                                                                                                                                      .build())
                                                                             .build();
            when(client.getServiceBindingForApplication(RANDOM_GUID, RANDOM_GUID)).thenReturn(serviceBinding);
            return;
        }
        when(client.getServiceBindingForApplication(RANDOM_GUID, RANDOM_GUID)).thenThrow(new CloudOperationException(HttpStatus.NOT_FOUND));
    }

}

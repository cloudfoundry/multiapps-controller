package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.cf.clients.AppBoundServiceInstanceNamesGetter;
import org.cloudfoundry.multiapps.controller.core.security.token.TokenService;
import org.cloudfoundry.multiapps.controller.persistence.services.FileStorageException;
import org.cloudfoundry.multiapps.controller.process.Messages;
import org.cloudfoundry.multiapps.controller.process.util.ServiceBindingParametersGetter;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;

import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceBinding;

class DetermineApplicationServiceBindingActionsStepTest extends SyncFlowableStepTest<DetermineApplicationServiceBindingActionsStep> {

    private static final String APP_NAME = "test_application";
    private static final String SERVICE_INSTANCE_NAME = "test_service";

    @Mock
    private ServiceBindingParametersGetter serviceBindingParametersGetter;
    @Mock
    private AppBoundServiceInstanceNamesGetter appServicesGetter;
    @Mock
    private TokenService tokenService;

    static Stream<Arguments> testDetermineServiceBindUnbind() {
        return Stream.of(
                         // (1) Service binding exist but service is no more part of the MTA
                         Arguments.of(false, false, true, true, false),
                         // (2) Service binding exist, service is no more part of the MTA and keepExistingBindings strategy is set to true
                         Arguments.of(false, true, true, false, false),
                         // (3) Service is part from MTA and binding doesn't exist
                         Arguments.of(true, false, false, false, true),
                         // (4) Service is part from MTA, it is already bound and existing parameters match to MTA parameters
                         Arguments.of(true, false, true, false, false),
                         // (5) Service is part of MTA and keepExistingBindings strategy is set to true
                         Arguments.of(true, true, true, false, false),
                         // (6) Service is not part of MTA and keepExistingBindings strategy is set to true
                         Arguments.of(false, true, true, false, false));
    }

    @ParameterizedTest
    @MethodSource
    void testDetermineServiceBindUnbind(boolean servicePartFromMta, boolean keepExistingBinding, boolean serviceBindingExist,
                                        boolean expectedUnbindValue, boolean expectedBindValue)
        throws FileStorageException {
        CloudApplicationExtended application = buildCloudApplicationExtended(servicePartFromMta, keepExistingBinding);
        prepareContext(application);
        prepareClient(application, serviceBindingExist);
        prepareServiceBindingParametersGetter(Collections.emptyMap(), Collections.emptyMap());

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertEquals(expectedUnbindValue, context.getVariable(Variables.SHOULD_UNBIND_SERVICE_FROM_APP), "Expected unbind value not match");
        assertEquals(expectedBindValue, context.getVariable(Variables.SHOULD_BIND_SERVICE_TO_APP), "Expected bind value not match");
    }

    @Test
    void testRebindServiceDueToChangedBindingParameters() throws FileStorageException {
        CloudApplicationExtended application = buildCloudApplicationExtended(true, false);
        prepareContext(application);
        prepareClient(application, true);
        Map<String, Object> mtaBindingParameters = Map.of("test-config", "test-value");
        prepareServiceBindingParametersGetter(mtaBindingParameters, null);

        step.execute(execution);

        assertStepFinishedSuccessfully();
        assertTrue(context.getVariable(Variables.SHOULD_UNBIND_SERVICE_FROM_APP), "Expected unbind value not match");
        assertTrue(context.getVariable(Variables.SHOULD_BIND_SERVICE_TO_APP), "Expected bind value not match");
    }

    @Test
    void testBindingToDeleteAndServiceToDeleteAreSet() {
        CloudServiceBinding serviceBindingToDelete = buildServiceBinding();
        context.setVariable(Variables.SERVICE_BINDING_TO_DELETE, serviceBindingToDelete);
        context.setVariable(Variables.SERVICE_TO_DELETE, SERVICE_INSTANCE_NAME);
        step.execute(execution);
        assertStepFinishedSuccessfully();
        assertTrue(context.getVariable(Variables.SHOULD_UNBIND_SERVICE_FROM_APP), "Expected unbind value not match");
        assertFalse(context.getVariable(Variables.SHOULD_BIND_SERVICE_TO_APP), "Expected bind value not match");
    }

    @Test
    void testBindingToDeleteAndAppToProcessAreSet() {
        CloudServiceBinding serviceBindingToDelete = buildServiceBinding();
        context.setVariable(Variables.SERVICE_BINDING_TO_DELETE, serviceBindingToDelete);
        CloudApplicationExtended appToProcess = buildCloudApplicationExtended(false, false);
        context.setVariable(Variables.APP_TO_PROCESS, appToProcess);
        step.execute(execution);
        assertStepFinishedSuccessfully();
        assertTrue(context.getVariable(Variables.SHOULD_UNBIND_SERVICE_FROM_APP), "Expected unbind value not match");
        assertFalse(context.getVariable(Variables.SHOULD_BIND_SERVICE_TO_APP), "Expected bind value not match");
    }

    @Test
    void testGetStepErrorMessageDuringServiceBindingDeletion() {
        CloudServiceBinding serviceBindingToDelete = buildServiceBinding();
        context.setVariable(Variables.SERVICE_BINDING_TO_DELETE, serviceBindingToDelete);
        assertEquals(MessageFormat.format(Messages.ERROR_WHILE_DETERMINING_BIND_UNBIND_OPERATIONS_OF_APPLICATION_GUID_TO_SERVICE_INSTANCE_GUID,
                                          serviceBindingToDelete.getApplicationGuid(), serviceBindingToDelete.getServiceInstanceGuid()),
                     step.getStepErrorMessage(context));
    }

    @Test
    void testGetStepErrorMessageDuringServiceBindingCreation() {
        CloudApplicationExtended app = buildCloudApplicationExtended(true, false);
        context.setVariable(Variables.APP_TO_PROCESS, app);
        context.setVariable(Variables.SERVICE_TO_UNBIND_BIND, SERVICE_INSTANCE_NAME);
        assertEquals(MessageFormat.format(Messages.ERROR_WHILE_DETERMINING_BIND_UNBIND_OPERATIONS_OF_APPLICATION_TO_SERVICE, APP_NAME,
                                          SERVICE_INSTANCE_NAME),
                     step.getStepErrorMessage(context));
    }

    private ImmutableCloudServiceBinding buildServiceBinding() {
        return ImmutableCloudServiceBinding.builder()
                                           .name("binding-to-delete")
                                           .applicationGuid(UUID.randomUUID())
                                           .serviceInstanceGuid(UUID.randomUUID())
                                           .build();
    }

    private CloudApplicationExtended buildCloudApplicationExtended(boolean servicePartFromMta, boolean keepExistingBinding) {
        CloudApplicationExtended.AttributeUpdateStrategy attributeUpdateStrategy = ImmutableCloudApplicationExtended.AttributeUpdateStrategy.builder()
                                                                                                                                            .shouldKeepExistingServiceBindings(keepExistingBinding)
                                                                                                                                            .build();
        ImmutableCloudApplicationExtended.Builder applicationBuilder = ImmutableCloudApplicationExtended.builder()
                                                                                                        .metadata(ImmutableCloudMetadata.of(UUID.randomUUID()))
                                                                                                        .name(APP_NAME)
                                                                                                        .attributesUpdateStrategy(attributeUpdateStrategy);
        return servicePartFromMta ? applicationBuilder.addService(SERVICE_INSTANCE_NAME)
                                                      .build()
            : applicationBuilder.build();
    }

    private void prepareContext(CloudApplicationExtended application) {
        context.setVariable(Variables.APP_TO_PROCESS, application);
        context.setVariable(Variables.SERVICE_TO_UNBIND_BIND, SERVICE_INSTANCE_NAME);
    }

    private void prepareClient(CloudApplicationExtended application, boolean serviceBindingExist) {
        when(client.getApplication(APP_NAME)).thenReturn(application);
        if (serviceBindingExist) {
            when(appServicesGetter.getServiceInstanceNamesBoundToApp(application.getGuid())).thenReturn(List.of(SERVICE_INSTANCE_NAME));
        }
    }

    private void prepareServiceBindingParametersGetter(Map<String, Object> mtaBindingParameters,
                                                       Map<String, Object> existingServiceBindingParameters)
        throws FileStorageException {
        when(serviceBindingParametersGetter.getServiceBindingParametersFromMta(any(), any())).thenReturn(mtaBindingParameters);
        when(serviceBindingParametersGetter.getServiceBindingParametersFromExistingInstance(any(),
                                                                                            any())).thenReturn(existingServiceBindingParameters);
    }

    @Override
    protected DetermineApplicationServiceBindingActionsStep createStep() {
        return new DetermineApplicationServiceBindingActionsStep() {
            @Override
            protected ServiceBindingParametersGetter getServiceBindingParametersGetter(ProcessContext context) {
                return serviceBindingParametersGetter;
            }

            @Override
            protected AppBoundServiceInstanceNamesGetter getAppServicesGetter(CloudCredentials credentials, String correlationId) {
                return appServicesGetter;
            }
        };
    }

}

package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.multiapps.common.ContentException;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerException;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.CloudServiceBrokerException;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBroker;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceBroker;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;

class CreateOrUpdateServiceBrokerStepTest extends SyncFlowableStepTest<CreateOrUpdateServiceBrokerStep> {

    private static final String APP_NAME = "foo";
    private static final String SERVICE_BROKER_NAME = "foo-broker";
    private static final String SERVICE_BROKER_NAME_2 = "boo-broker";
    private static final String SERVICE_BROKER_NAME_3 = "bar-broker";
    private static final String SERVICE_BROKER_USER = "bar";
    private static final String SERVICE_BROKER_OLD_USER = "old-username";
    private static final String SERVICE_BROKER_PASSWORD = "baz";
    private static final String SERVICE_BROKER_OLD_PASSWORD = "old-password";
    private static final String SERVICE_BROKER_URL = "http://localhost:3030";
    private static final String SERVICE_BROKER_URL_2 = "http://localhost:3031";

    @InjectMocks
    private final CreateOrUpdateServiceBrokerStep step = new CreateOrUpdateServiceBrokerStep();

    public static Stream<Arguments> testExecute() {
        return Stream.of(
// @formatter:off
            // (1) A service broker should be created, all necessary parameters are present and it isn't space scoped (explicit):
            Arguments.of(new StepInput(new SimpleApplication(APP_NAME, Map.of(SupportedParameters.CREATE_SERVICE_BROKER, true, SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                           SupportedParameters.SERVICE_BROKER_USERNAME, SERVICE_BROKER_USER, 
                                                                           SupportedParameters.SERVICE_BROKER_PASSWORD, SERVICE_BROKER_PASSWORD,
                                                                           SupportedParameters.SERVICE_BROKER_URL, SERVICE_BROKER_URL,
                                                                           SupportedParameters.SERVICE_BROKER_SPACE_SCOPED, false))),
                         new StepOutput(ImmutableCloudServiceBroker.builder().password(SERVICE_BROKER_PASSWORD).url(SERVICE_BROKER_URL).username(SERVICE_BROKER_USER).name(SERVICE_BROKER_NAME).build(), false),
                         null, null, null, null, null),
            // (2) A service broker should be created, all necessary parameters are present and it isn't space scoped (implicit):
            Arguments.of(new StepInput(new SimpleApplication(APP_NAME, Map.of(SupportedParameters.CREATE_SERVICE_BROKER, true, SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                              SupportedParameters.SERVICE_BROKER_USERNAME, SERVICE_BROKER_USER, 
                                                                              SupportedParameters.SERVICE_BROKER_PASSWORD, SERVICE_BROKER_PASSWORD,
                                                                              SupportedParameters.SERVICE_BROKER_URL, SERVICE_BROKER_URL))),
                            new StepOutput(ImmutableCloudServiceBroker.builder().password(SERVICE_BROKER_PASSWORD).url(SERVICE_BROKER_URL).username(SERVICE_BROKER_USER).name(SERVICE_BROKER_NAME).build(), false),
                            null, null, null, null, null),
            // (3) No service brokers should be created (implicit):
            Arguments.of(new StepInput(new SimpleApplication(APP_NAME, Collections.emptyMap())), null, null, null, null, null, null),
            // (4) No service brokers should be created (explicit):
            Arguments.of(new StepInput(new SimpleApplication(APP_NAME, Map.of(SupportedParameters.CREATE_SERVICE_BROKER, false))), null, null, null, null, null, null),
            // (5) A service broker should be created but the username is missing:
            Arguments.of(new StepInput(new SimpleApplication(APP_NAME, Map.of(SupportedParameters.CREATE_SERVICE_BROKER, true, SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                              SupportedParameters.SERVICE_BROKER_PASSWORD, SERVICE_BROKER_PASSWORD,
                                                                              SupportedParameters.SERVICE_BROKER_URL, SERVICE_BROKER_URL))),
                         null, null, "Missing service broker username for application \"foo\"", ContentException.class, null, null),
            // (6) A service broker should be created and the password is missing:
            Arguments.of(new StepInput(new SimpleApplication(APP_NAME, Map.of(SupportedParameters.CREATE_SERVICE_BROKER, true, SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                              SupportedParameters.SERVICE_BROKER_USERNAME, SERVICE_BROKER_USER,
                                                                              SupportedParameters.SERVICE_BROKER_URL, SERVICE_BROKER_URL))),
                         null, null, "Missing service broker password for application \"foo\"", ContentException.class, null, null),
            // (7) A service broker should be created but the url is missing:
            Arguments.of(new StepInput(new SimpleApplication(APP_NAME, Map.of(SupportedParameters.CREATE_SERVICE_BROKER, true, SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                              SupportedParameters.SERVICE_BROKER_USERNAME, SERVICE_BROKER_USER, 
                                                                              SupportedParameters.SERVICE_BROKER_PASSWORD, SERVICE_BROKER_PASSWORD))),
                         null, null, "Missing service broker url for application \"foo\"", ContentException.class, null, null),
            // (8) A service broker should be created and the name is missing:
            Arguments.of(new StepInput(new SimpleApplication(APP_NAME, Map.of(SupportedParameters.CREATE_SERVICE_BROKER, true,
                                                                              SupportedParameters.SERVICE_BROKER_USERNAME, SERVICE_BROKER_USER, 
                                                                              SupportedParameters.SERVICE_BROKER_PASSWORD, SERVICE_BROKER_PASSWORD,
                                                                              SupportedParameters.SERVICE_BROKER_URL, SERVICE_BROKER_URL))),
                         new StepOutput(ImmutableCloudServiceBroker.builder().password(SERVICE_BROKER_PASSWORD).url(SERVICE_BROKER_URL).username(SERVICE_BROKER_USER).name(APP_NAME).build(), false),
                         null, null, null, null, null),
            // (9) A service broker should be updated and all necessary parameters are present:
            Arguments.of(new StepInput(ImmutableCloudServiceBroker.builder().name(SERVICE_BROKER_NAME).build(),
                                       new SimpleApplication(APP_NAME, Map.of(SupportedParameters.CREATE_SERVICE_BROKER, true, SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                              SupportedParameters.SERVICE_BROKER_USERNAME, SERVICE_BROKER_USER, 
                                                                              SupportedParameters.SERVICE_BROKER_PASSWORD, SERVICE_BROKER_PASSWORD,
                                                                              SupportedParameters.SERVICE_BROKER_URL, SERVICE_BROKER_URL)), null),
                         new StepOutput(ImmutableCloudServiceBroker.builder().password(SERVICE_BROKER_PASSWORD).url(SERVICE_BROKER_URL).username(SERVICE_BROKER_USER).name(SERVICE_BROKER_NAME).build(), true),
                         null, null, null, null, null),
            // (10) Update call for broker should be made, although update throws exception:
            Arguments.of(new StepInput(ImmutableCloudServiceBroker.builder().name(SERVICE_BROKER_NAME).build(),
                                       new SimpleApplication(APP_NAME, Map.of(SupportedParameters.CREATE_SERVICE_BROKER, true, SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                              SupportedParameters.SERVICE_BROKER_USERNAME, SERVICE_BROKER_USER, 
                                                                              SupportedParameters.SERVICE_BROKER_PASSWORD, SERVICE_BROKER_PASSWORD,
                                                                              SupportedParameters.SERVICE_BROKER_URL, SERVICE_BROKER_URL)), null),
                         new StepOutput(ImmutableCloudServiceBroker.builder().password(SERVICE_BROKER_PASSWORD).url(SERVICE_BROKER_URL).username(SERVICE_BROKER_USER).name(SERVICE_BROKER_NAME).build(), true),
                         "Could not update service broker \"foo-broker\". Operation not supported.",
                         null, null, null, new CloudOperationException(HttpStatus.NOT_IMPLEMENTED)),
            // (11) A random exception is thrown during create:
            Arguments.of(new StepInput(ImmutableCloudServiceBroker.builder().name(SERVICE_BROKER_NAME).build(),
                                       new SimpleApplication(APP_NAME, Map.of(SupportedParameters.CREATE_SERVICE_BROKER, true, SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME_2,
                                                                              SupportedParameters.SERVICE_BROKER_USERNAME, SERVICE_BROKER_USER, 
                                                                              SupportedParameters.SERVICE_BROKER_PASSWORD, SERVICE_BROKER_PASSWORD,
                                                                              SupportedParameters.SERVICE_BROKER_URL, SERVICE_BROKER_URL)), null),
                         null, null, "Controller operation failed: 418 I'm a teapot", CloudControllerException.class, new CloudOperationException(HttpStatus.I_AM_A_TEAPOT), null),
            // (12) A random exception is thrown during update:
            Arguments.of(new StepInput(ImmutableCloudServiceBroker.builder().name(SERVICE_BROKER_NAME_3).build(),
                                       new SimpleApplication(APP_NAME, Map.of(SupportedParameters.CREATE_SERVICE_BROKER, true, SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME_3,
                                                                              SupportedParameters.SERVICE_BROKER_USERNAME, SERVICE_BROKER_USER, 
                                                                              SupportedParameters.SERVICE_BROKER_PASSWORD, SERVICE_BROKER_PASSWORD,
                                                                              SupportedParameters.SERVICE_BROKER_URL, SERVICE_BROKER_URL_2)), null),
                         null, null, "Controller operation failed: 418 I'm a teapot", CloudControllerException.class, null, new CloudOperationException(HttpStatus.I_AM_A_TEAPOT)),
            // (13) Create/update calls for should fail, because both create and update throw an exception and failsafe option is not set: 
            Arguments.of(new StepInput(ImmutableCloudServiceBroker.builder().name(SERVICE_BROKER_NAME).build(),
                                       new SimpleApplication(APP_NAME, Map.of(SupportedParameters.CREATE_SERVICE_BROKER, true, SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                              SupportedParameters.SERVICE_BROKER_USERNAME, SERVICE_BROKER_USER, 
                                                                              SupportedParameters.SERVICE_BROKER_PASSWORD, SERVICE_BROKER_PASSWORD,
                                                                              SupportedParameters.SERVICE_BROKER_URL, SERVICE_BROKER_URL)), null),
                         new StepOutput(ImmutableCloudServiceBroker.builder().password(SERVICE_BROKER_PASSWORD).url(SERVICE_BROKER_URL).username(SERVICE_BROKER_USER).name(SERVICE_BROKER_NAME).build(), true),
                         null, "Service broker operation failed: 403 Forbidden", CloudServiceBrokerException.class, new CloudOperationException(HttpStatus.FORBIDDEN), new CloudOperationException(HttpStatus.FORBIDDEN)),
            // (14) Update call for broker should be made, although both create and update throw an exception but failsafe option is set: 
            Arguments.of(new StepInput(ImmutableCloudServiceBroker.builder().name(SERVICE_BROKER_NAME).build(),
                                       new SimpleApplication(APP_NAME, Map.of(SupportedParameters.CREATE_SERVICE_BROKER, true, SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                              SupportedParameters.SERVICE_BROKER_USERNAME, SERVICE_BROKER_USER, 
                                                                              SupportedParameters.SERVICE_BROKER_PASSWORD, SERVICE_BROKER_PASSWORD,
                                                                              SupportedParameters.SERVICE_BROKER_URL, SERVICE_BROKER_URL)), null),
                         new StepOutput(ImmutableCloudServiceBroker.builder().password(SERVICE_BROKER_PASSWORD).url(SERVICE_BROKER_URL).username(SERVICE_BROKER_USER).name(SERVICE_BROKER_NAME).build(), true),
                         "Could not update service broker \"foo-broker\". Operation forbidden. Only admin users can manage service brokers!",
                         null, null, new CloudOperationException(HttpStatus.FORBIDDEN), new CloudOperationException(HttpStatus.FORBIDDEN)),
            // (15) Create call for broker should be made, although both create and update throw an exception but failsafe option is set: 
            Arguments.of(new StepInput(ImmutableCloudServiceBroker.builder().name(SERVICE_BROKER_NAME).build(),
                                       new SimpleApplication(APP_NAME, Map.of(SupportedParameters.CREATE_SERVICE_BROKER, true, SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME_2,
                                                                              SupportedParameters.SERVICE_BROKER_USERNAME, SERVICE_BROKER_USER, 
                                                                              SupportedParameters.SERVICE_BROKER_PASSWORD, SERVICE_BROKER_PASSWORD,
                                                                              SupportedParameters.SERVICE_BROKER_URL, SERVICE_BROKER_URL)), null),
                         new StepOutput(ImmutableCloudServiceBroker.builder().password(SERVICE_BROKER_PASSWORD).url(SERVICE_BROKER_URL).username(SERVICE_BROKER_USER).name(SERVICE_BROKER_NAME_2).build(), false),
                         "Could not create service broker \"boo-broker\". Operation forbidden. Only admin users can manage service brokers!", null, null, new CloudOperationException(HttpStatus.FORBIDDEN), new CloudOperationException(HttpStatus.FORBIDDEN)),
            // (16) Create call for broker fails with forbidden and failsafe option is not set:
            Arguments.of(new StepInput(new SimpleApplication(APP_NAME, Map.of(SupportedParameters.CREATE_SERVICE_BROKER, true, SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME_2,
                                                                              SupportedParameters.SERVICE_BROKER_USERNAME, SERVICE_BROKER_USER, 
                                                                              SupportedParameters.SERVICE_BROKER_PASSWORD, SERVICE_BROKER_PASSWORD,
                                                                              SupportedParameters.SERVICE_BROKER_URL, SERVICE_BROKER_URL)), null),
                         new StepOutput(ImmutableCloudServiceBroker.builder().password(SERVICE_BROKER_PASSWORD).url(SERVICE_BROKER_URL).username(SERVICE_BROKER_USER).name(SERVICE_BROKER_NAME_2).build(), false),
                         null, "Error creating service brokers: Service broker operation failed: 403 Forbidden", CloudServiceBrokerException.class, new CloudOperationException(HttpStatus.FORBIDDEN), null),
            // (17) Create call for broker fails with bad gateway:
            Arguments.of(new StepInput(new SimpleApplication(APP_NAME, Map.of(SupportedParameters.CREATE_SERVICE_BROKER, true, SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME_2,
                                                                              SupportedParameters.SERVICE_BROKER_USERNAME, SERVICE_BROKER_USER, 
                                                                              SupportedParameters.SERVICE_BROKER_PASSWORD, SERVICE_BROKER_PASSWORD,
                                                                              SupportedParameters.SERVICE_BROKER_URL, SERVICE_BROKER_URL)), null),
                         new StepOutput(ImmutableCloudServiceBroker.builder().password(SERVICE_BROKER_PASSWORD).url(SERVICE_BROKER_URL).username(SERVICE_BROKER_USER).name(SERVICE_BROKER_NAME_2).build(), false),
                         null, "Service broker operation failed: 502 Bad Gateway", CloudServiceBrokerException.class, new CloudOperationException(HttpStatus.BAD_GATEWAY), null),
            // (18) Update call for broker fails with bad gateway:
            Arguments.of(new StepInput(ImmutableCloudServiceBroker.builder().name(SERVICE_BROKER_NAME_2).build(),
                                       new SimpleApplication(APP_NAME, Map.of(SupportedParameters.CREATE_SERVICE_BROKER, true, SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME_2,
                                                                              SupportedParameters.SERVICE_BROKER_USERNAME, SERVICE_BROKER_USER, 
                                                                              SupportedParameters.SERVICE_BROKER_PASSWORD, SERVICE_BROKER_PASSWORD,
                                                                              SupportedParameters.SERVICE_BROKER_URL, SERVICE_BROKER_URL)), null),
                         new StepOutput(ImmutableCloudServiceBroker.builder().password(SERVICE_BROKER_PASSWORD).url(SERVICE_BROKER_URL).username(SERVICE_BROKER_USER).name(SERVICE_BROKER_NAME_2).build(), true),
                         null, "Service broker operation failed: 502 Bad Gateway", CloudServiceBrokerException.class, null, new CloudOperationException(HttpStatus.BAD_GATEWAY)),
            
            // (19) A service broker should be created, all necessary parameters are present and it is space scoped:
            Arguments.of(new StepInput(new SimpleApplication(APP_NAME, Map.of(SupportedParameters.CREATE_SERVICE_BROKER, true, SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                              SupportedParameters.SERVICE_BROKER_USERNAME, SERVICE_BROKER_USER, 
                                                                              SupportedParameters.SERVICE_BROKER_PASSWORD, SERVICE_BROKER_PASSWORD,
                                                                              SupportedParameters.SERVICE_BROKER_URL, SERVICE_BROKER_URL,
                                                                              SupportedParameters.SERVICE_BROKER_SPACE_SCOPED, true)), "cb42bf12-b819-4c27-5ba0-d3dc351b80dc"),
                         new StepOutput(ImmutableCloudServiceBroker.builder().password(SERVICE_BROKER_PASSWORD).url(SERVICE_BROKER_URL).username(SERVICE_BROKER_USER).name(SERVICE_BROKER_NAME).spaceGuid("cb42bf12-b819-4c27-5ba0-d3dc351b80dc").build(), false),
                         null, null, null, null, null),
            // (20) The visibility of a service broker should be changed from global to space-scoped:
            Arguments.of(new StepInput(ImmutableCloudServiceBroker.builder().name(SERVICE_BROKER_NAME).username(SERVICE_BROKER_OLD_USER).password(SERVICE_BROKER_OLD_PASSWORD).build(),
                                       new SimpleApplication(APP_NAME, Map.of(SupportedParameters.CREATE_SERVICE_BROKER, true, SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                              SupportedParameters.SERVICE_BROKER_USERNAME, SERVICE_BROKER_USER, 
                                                                              SupportedParameters.SERVICE_BROKER_PASSWORD, SERVICE_BROKER_PASSWORD,
                                                                              SupportedParameters.SERVICE_BROKER_URL, SERVICE_BROKER_URL,
                                                                              SupportedParameters.SERVICE_BROKER_SPACE_SCOPED, true)), "cb42bf12-b819-4c27-5ba0-d3dc351b80dc"),
                         new StepOutput(ImmutableCloudServiceBroker.builder().password(SERVICE_BROKER_PASSWORD).url(SERVICE_BROKER_URL).username(SERVICE_BROKER_USER).name(SERVICE_BROKER_NAME).spaceGuid("cb42bf12-b819-4c27-5ba0-d3dc351b80dc").build(), true),
                         "Visibility of service broker \"foo-broker\" will not be changed from global to space-scoped, as visibility changes are not yet supported!", null, null, null, null),
            // (21) The visibility of a service broker should be changed from space-scoped to global:
            Arguments.of(new StepInput(ImmutableCloudServiceBroker.builder().name(SERVICE_BROKER_NAME).username(SERVICE_BROKER_OLD_USER).password(SERVICE_BROKER_OLD_PASSWORD).spaceGuid("cb42bf12-b819-4c27-5ba0-d3dc351b80dc").build(),
                                       new SimpleApplication(APP_NAME, Map.of(SupportedParameters.CREATE_SERVICE_BROKER, true, SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                              SupportedParameters.SERVICE_BROKER_USERNAME, SERVICE_BROKER_USER, 
                                                                              SupportedParameters.SERVICE_BROKER_PASSWORD, SERVICE_BROKER_PASSWORD,
                                                                              SupportedParameters.SERVICE_BROKER_URL, SERVICE_BROKER_URL,
                                                                              SupportedParameters.SERVICE_BROKER_SPACE_SCOPED, false)), null),
                         new StepOutput(ImmutableCloudServiceBroker.builder().password(SERVICE_BROKER_PASSWORD).url(SERVICE_BROKER_URL).username(SERVICE_BROKER_USER).name(SERVICE_BROKER_NAME).build(), true),
                         "Visibility of service broker \"foo-broker\" will not be changed from space-scoped to global, as visibility changes are not yet supported!", null, null, null, null),
            // (22) Create service broker with async job
            Arguments.of(new StepInput(null, new SimpleApplication(APP_NAME, Map.of(SupportedParameters.CREATE_SERVICE_BROKER, true, SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                              SupportedParameters.SERVICE_BROKER_USERNAME, SERVICE_BROKER_USER, 
                                                                              SupportedParameters.SERVICE_BROKER_PASSWORD, SERVICE_BROKER_PASSWORD,
                                                                              SupportedParameters.SERVICE_BROKER_URL, SERVICE_BROKER_URL)), null, "1"),
                         new StepOutput(ImmutableCloudServiceBroker.builder().password(SERVICE_BROKER_PASSWORD).url(SERVICE_BROKER_URL).username(SERVICE_BROKER_USER).name(SERVICE_BROKER_NAME).build(),
                                        false), null, null, null, null, null),
            // (23) Update service broker with async job
            Arguments.of(new StepInput(ImmutableCloudServiceBroker.builder().password(SERVICE_BROKER_OLD_PASSWORD).url(SERVICE_BROKER_URL_2).username(SERVICE_BROKER_OLD_USER).name(SERVICE_BROKER_NAME).build(), 
                                       new SimpleApplication(APP_NAME, Map.of(SupportedParameters.CREATE_SERVICE_BROKER, true, SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                                    SupportedParameters.SERVICE_BROKER_USERNAME, SERVICE_BROKER_USER, 
                                                                                    SupportedParameters.SERVICE_BROKER_PASSWORD, SERVICE_BROKER_PASSWORD,
                                                                                    SupportedParameters.SERVICE_BROKER_URL, SERVICE_BROKER_URL)), null, "2"),
                               new StepOutput(ImmutableCloudServiceBroker.builder().password(SERVICE_BROKER_PASSWORD).url(SERVICE_BROKER_URL).username(SERVICE_BROKER_USER).name(SERVICE_BROKER_NAME).build(),
                                              true), null, null, null, null, null)
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(StepInput stepInput, StepOutput expectedOutput, String expectedWarningMessage, String expectedExceptionMessage,
                     Class<? extends Exception> expectedExceptionClass, CloudOperationException createException,
                     CloudOperationException updateException) {
        initializeParameters(createException, updateException, stepInput);
        if (expectedExceptionMessage != null) {
            validateExceptionIsThrown(expectedExceptionMessage, expectedExceptionClass);
            return;
        }

        context.setVariable(Variables.NO_FAIL_ON_MISSING_PERMISSIONS, true);
        step.execute(execution);

        assertStepPhaseStatus(stepInput.jobId);
        validateStepOutput(expectedOutput);
        applyExpectedWarningMessage(expectedWarningMessage);
        validateServiceBrokers(expectedOutput);
        assertEquals(stepInput.jobId, context.getVariable(Variables.SERVICE_BROKER_ASYNC_JOB_ID));
    }

    private void initializeParameters(CloudOperationException createException, CloudOperationException updateException, StepInput input) {
        var cloudApp = input.application.toCloudApplication();
        prepareContext(input, cloudApp);
        prepareClient(createException, updateException, input, cloudApp);
    }

    private void prepareContext(StepInput input, CloudApplicationExtended app) {
        context.setVariable(Variables.APP_TO_PROCESS, app);
        context.setVariable(Variables.SPACE_GUID, input.spaceGuid);
    }

    private void prepareClient(CloudOperationException createException, CloudOperationException updateException, StepInput input,
                               CloudApplicationExtended app) {
        Mockito.when(client.getServiceBrokers())
               .thenReturn(Optional.ofNullable(input.existingServiceBroker)
                                   .map(List::of)
                                   .orElse(Collections.emptyList()));
        if (updateException != null) {
            Mockito.doThrow(updateException)
                   .when(client)
                   .updateServiceBroker(Mockito.any());
        } else {
            Mockito.when(client.updateServiceBroker(Mockito.any()))
                   .thenReturn(input.jobId);
        }
        if (createException != null) {
            Mockito.doThrow(createException)
                   .when(client)
                   .createServiceBroker(Mockito.any());
        } else {
            Mockito.when(client.createServiceBroker(Mockito.any()))
                   .thenReturn(input.jobId);
        }
        Mockito.when(client.getApplication(input.application.name))
               .thenReturn(app);
        Mockito.when(client.getApplicationEnvironment(Mockito.eq(app.getGuid())))
               .thenReturn(app.getEnv());
    }

    private void validateExceptionIsThrown(String expectedExceptionMessage, Class<? extends Exception> expectedExceptionClass) {
        context.setVariable(Variables.NO_FAIL_ON_MISSING_PERMISSIONS, false);
        Exception exception = assertThrows(SLException.class, () -> step.execute(execution));
        assertEquals(expectedExceptionClass, exception.getCause()
                                                      .getClass());
        assertTrue(exception.getMessage()
                            .contains(expectedExceptionMessage),
                   MessageFormat.format("Expected to contain \"{0}\" but was \"{1}\"", expectedExceptionMessage, exception.getMessage()));
    }

    private void assertStepPhaseStatus(String jobId) {
        if (jobId == null) {
            assertStepFinishedSuccessfully();
            return;
        }
        assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
    }

    private void validateStepOutput(StepOutput expectedOutput) {
        StepOutput actualOutput = new StepOutput();

        ArgumentCaptor<CloudServiceBroker> createArgumentCaptor = ArgumentCaptor.forClass(CloudServiceBroker.class);
        boolean expectCreateServiceBroker = expectedOutput != null && !expectedOutput.updatedServiceBroker;
        if (expectCreateServiceBroker) {
            Mockito.verify(client, Mockito.times(1))
                   .createServiceBroker(createArgumentCaptor.capture());
            actualOutput.serviceBroker = createArgumentCaptor.getValue();
        } else {
            Mockito.verify(client, Mockito.never())
                   .createServiceBroker(Mockito.any());
        }

        ArgumentCaptor<CloudServiceBroker> updateArgumentCaptor = ArgumentCaptor.forClass(CloudServiceBroker.class);
        boolean expectUpdateServiceBroker = expectedOutput != null && expectedOutput.updatedServiceBroker;
        if (expectUpdateServiceBroker) {
            Mockito.verify(client, Mockito.times(1))
                   .updateServiceBroker(updateArgumentCaptor.capture());
            actualOutput.serviceBroker = updateArgumentCaptor.getValue();
            actualOutput.updatedServiceBroker = true;
        } else {
            Mockito.verify(client, Mockito.never())
                   .updateServiceBroker(Mockito.any());
        }

        if (expectedOutput != null) {
            assertEquals(JsonUtil.toJson(expectedOutput, true), JsonUtil.toJson(actualOutput, true));
        }
    }

    private void applyExpectedWarningMessage(String expectedWarningMessage) {
        if (expectedWarningMessage != null) {
            Mockito.verify(stepLogger)
                   .warn(expectedWarningMessage);
        }
    }

    private void validateServiceBrokers(StepOutput expectedOutput) {
        if (expectedOutput == null) {
            return;
        }

        CloudServiceBroker actuallyCreatedOrUpdatedServiceBroker = context.getVariable(Variables.CREATED_OR_UPDATED_SERVICE_BROKER);
        CloudServiceBroker expectedserviceBroker = expectedOutput.serviceBroker;
        assertEquals(JsonUtil.toJson(expectedserviceBroker, true), JsonUtil.toJson(actuallyCreatedOrUpdatedServiceBroker, true));
    }

    private static class StepInput {
        CloudServiceBroker existingServiceBroker;
        SimpleApplication application;
        String spaceGuid;
        String jobId;

        StepInput(SimpleApplication application) {
            this.application = application;
        }

        StepInput(SimpleApplication application, String spaceGuid) {
            this.application = application;
            this.spaceGuid = spaceGuid;
        }

        StepInput(CloudServiceBroker existingServiceBroker, SimpleApplication application, String spaceGuid) {
            this.existingServiceBroker = existingServiceBroker;
            this.application = application;
            this.spaceGuid = spaceGuid;
        }

        StepInput(CloudServiceBroker existingServiceBroker, SimpleApplication application, String spaceGuid, String jobId) {
            this.existingServiceBroker = existingServiceBroker;
            this.application = application;
            this.spaceGuid = spaceGuid;
            this.jobId = jobId;
        }

    }

    private static class StepOutput {
        CloudServiceBroker serviceBroker;
        boolean updatedServiceBroker;

        StepOutput() {
            this.serviceBroker = null;
            this.updatedServiceBroker = false;
        }

        StepOutput(CloudServiceBroker serviceBroker, boolean updatedServiceBroker) {
            this.serviceBroker = serviceBroker;
            this.updatedServiceBroker = updatedServiceBroker;
        }

    }

    static class SimpleApplication {

        String name;
        Map<String, Object> attributes;

        SimpleApplication(String name, Map<String, Object> attributes) {
            this.name = name;
            this.attributes = attributes;
        }

        CloudApplicationExtended toCloudApplication() {
            return ImmutableCloudApplicationExtended.builder()
                                                    .metadata(ImmutableCloudMetadata.of(UUID.randomUUID()))
                                                    .name(name)
                                                    .env(Map.of(org.cloudfoundry.multiapps.controller.core.Constants.ENV_DEPLOY_ATTRIBUTES,
                                                                JsonUtil.toJson(attributes)))
                                                    .build();
        }

    }

    @Override
    protected CreateOrUpdateServiceBrokerStep createStep() {
        return new CreateOrUpdateServiceBrokerStep();
    }

}

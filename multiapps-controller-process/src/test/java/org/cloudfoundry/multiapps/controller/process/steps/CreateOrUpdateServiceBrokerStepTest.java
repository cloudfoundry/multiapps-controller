package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.client.lib.domain.CloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.client.lib.domain.ImmutableCloudApplicationExtended;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

class CreateOrUpdateServiceBrokerStepTest extends SyncFlowableStepTest<CreateOrUpdateServiceBrokerStep> {

    @InjectMocks
    private final CreateOrUpdateServiceBrokerStep step = new CreateOrUpdateServiceBrokerStep();

    public static Stream<Arguments> testExecute() {
        return Stream.of(
// @formatter:off
            // (00) A service broker should be created, all necessary parameters are present and it isn't space scoped (explicit):
            Arguments.of("create-service-brokers-step-input-00.json", "create-service-brokers-step-output-01.json", null, null, null, null, null),
            // (01) A service broker should be created, all necessary parameters are present and it isn't space scoped (implicit):
            Arguments.of("create-service-brokers-step-input-01.json", "create-service-brokers-step-output-01.json", null, null, null, null, null),
            // (02) No service brokers should be created (implicit):
            Arguments.of("create-service-brokers-step-input-02.json", "create-service-brokers-step-output-02.json", null, null, null, null, null),
            // (03) No service brokers should be created (explicit):
            Arguments.of("create-service-brokers-step-input-03.json", "create-service-brokers-step-output-02.json", null, null, null, null, null),
            // (04) A service broker should be created but the username is missing:
            Arguments.of("create-service-brokers-step-input-04.json", null, null, "Missing service broker username for application \"foo\"", null, null, null),
            // (05) A service broker should be created and the password is missing:
            Arguments.of("create-service-brokers-step-input-05.json", "create-service-brokers-step-output-05.json", null, "Missing service broker password for application \"foo\"", null, null, null),
            // (06) A service broker should be created but the url is missing:
            Arguments.of("create-service-brokers-step-input-06.json", null, null, "Missing service broker url for application \"foo\"", null, null, null),
            // (07) A service broker should be created and the name is missing:
            Arguments.of("create-service-brokers-step-input-07.json", "create-service-brokers-step-output-07.json", null, null, null, null, null),
            // (08) A service broker should be updated and all necessary parameters are present:
            Arguments.of("create-service-brokers-step-input-08.json", "create-service-brokers-step-output-08.json", null, null, null, null, null),
            // (09) Update call for broker should be made, although update throws exception:
            Arguments.of("create-service-brokers-step-input-09.json", "create-service-brokers-step-output-09.json", "Could not update service broker \"foo-broker\". Operation not supported.", null, null, null, new CloudOperationException(HttpStatus.NOT_IMPLEMENTED)),
            // (10) A random exception is thrown during create:
            Arguments.of("create-service-brokers-step-input-15.json", null, null, "Controller operation failed: 418 I'm a teapot", SLException.class, new CloudOperationException(HttpStatus.I_AM_A_TEAPOT), null),
            // (11) A random exception is thrown during update:
            Arguments.of("create-service-brokers-step-input-14.json", null, null, "Controller operation failed: 418 I'm a teapot", SLException.class, null, new CloudOperationException(HttpStatus.I_AM_A_TEAPOT)),
            // (12) Create/update calls for should fail, because both create and update throw an exception and failsafe option is not set: 
            Arguments.of("create-service-brokers-step-input-09.json", "create-service-brokers-step-output-09.json", null, "Service broker operation failed: 403 Forbidden", SLException.class, new CloudOperationException(HttpStatus.FORBIDDEN), new CloudOperationException(HttpStatus.FORBIDDEN)),
            // (13) Update call for broker should be made, although both create and update throw an exception but failsafe option is set: 
            Arguments.of("create-service-brokers-step-input-16.json", "create-service-brokers-step-output-09.json", "Could not update service broker \"foo-broker\". Operation forbidden. Only admin users can manage service brokers!", null, null, new CloudOperationException(HttpStatus.FORBIDDEN), new CloudOperationException(HttpStatus.FORBIDDEN)),
            // (14) Create call for broker should be made, although both create and update throw an exception but failsafe option is set: 
            Arguments.of("create-service-brokers-step-input-15.json", "create-service-brokers-step-output-13.json", "Could not create service broker \"boo-broker\". Operation forbidden. Only admin users can manage service brokers!", null, null, new CloudOperationException(HttpStatus.FORBIDDEN), new CloudOperationException(HttpStatus.FORBIDDEN)),
            // (15) A service broker should be created, all necessary parameters are present and it is space scoped:
            Arguments.of("create-service-brokers-step-input-10.json", "create-service-brokers-step-output-10.json", null, null, null, null, null),
            // (16) The visibility of a service broker should be changed from global to space-scoped:
            Arguments.of("create-service-brokers-step-input-11.json", "create-service-brokers-step-output-11.json", "Visibility of service broker \"foo-broker\" will not be changed from global to space-scoped, as visibility changes are not yet supported!", null, null, null, null),
            // (17) The visibility of a service broker should be changed from space-scoped to global:
            Arguments.of("create-service-brokers-step-input-12.json", "create-service-brokers-step-output-12.json", "Visibility of service broker \"foo-broker\" will not be changed from space-scoped to global, as visibility changes are not yet supported!", null, null, null, null)
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(String inputLocation, String expectedOutputLocation, String expectedWarningMessage, String expectedExceptionMessage,
                     Class<? extends Throwable> expectedExceptionClass, CloudOperationException createException,
                     CloudOperationException updateException) {
        initializeParameters(createException, updateException,
                             JsonUtil.fromJson(TestUtil.getResourceAsString(inputLocation, getClass()), StepInput.class));
        if (expectedExceptionMessage != null) {
            validateExceptionIsThrown(expectedExceptionMessage, expectedExceptionClass);
            return;
        }

        context.setVariable(Variables.NO_FAIL_ON_MISSING_PERMISSIONS, true);
        step.execute(execution);
        assertStepFinishedSuccessfully();

        StepOutput expectedOutput = JsonUtil.fromJson(TestUtil.getResourceAsString(expectedOutputLocation, getClass()), StepOutput.class);
        StepOutput actualOutput = captureStepOutput(expectedOutput);
        assertEquals(JsonUtil.toJson(expectedOutput, true), JsonUtil.toJson(actualOutput, true));
        applyExpectedWarningMessage(expectedWarningMessage);
        validateServiceBrokers(expectedOutput);
    }

    private void initializeParameters(CloudOperationException createException, CloudOperationException updateException, StepInput input) {
        prepareContext(input);
        prepareClient(createException, updateException, input);
    }

    private void prepareContext(StepInput input) {
        context.setVariable(Variables.APP_TO_PROCESS, input.application.toCloudApplication());
        context.setVariable(Variables.SPACE_GUID, input.spaceGuid);
    }

    private void prepareClient(CloudOperationException createException, CloudOperationException updateException, StepInput input) {
        Mockito.when(client.getServiceBrokers())
               .thenReturn(input.existingServiceBrokers);
        if (updateException != null) {
            Mockito.doThrow(updateException)
                   .when(client)
                   .updateServiceBroker(Mockito.any());
        }
        if (createException != null) {
            Mockito.doThrow(createException)
                   .when(client)
                   .createServiceBroker(Mockito.any());
        }
        Mockito.when(client.getApplication(input.application.name))
               .thenReturn(input.application.toCloudApplication());
    }

    private void validateExceptionIsThrown(String expectedExceptionMessage, Class<? extends Throwable> expectedExceptionClass) {
        context.setVariable(Variables.NO_FAIL_ON_MISSING_PERMISSIONS, false);
        Throwable throwable = assertThrows(parseExpectedExceptionClass(expectedExceptionClass), () -> step.execute(execution));
        assertTrue(throwable.getMessage()
                            .contains(expectedExceptionMessage));
    }

    private Class<? extends Throwable> parseExpectedExceptionClass(Class<? extends Throwable> expectedExceptionClass) {
        return (expectedExceptionClass != null) ? expectedExceptionClass : SLException.class;
    }

    private StepOutput captureStepOutput(StepOutput expectedOutput) {

        StepOutput actualOutput = new StepOutput();

        ArgumentCaptor<CloudServiceBroker> createArgumentCaptor = ArgumentCaptor.forClass(CloudServiceBroker.class);
        boolean expectCreateServiceBroker = expectedOutput != null && expectedOutput.createdServiceBroker != null;
        if (expectCreateServiceBroker) {
            Mockito.verify(client, Mockito.times(1))
                   .createServiceBroker(createArgumentCaptor.capture());
            actualOutput.createdServiceBroker = createArgumentCaptor.getValue();
        } else {
            Mockito.verify(client, Mockito.never())
                   .createServiceBroker(Mockito.any());
        }

        ArgumentCaptor<CloudServiceBroker> updateArgumentCaptor = ArgumentCaptor.forClass(CloudServiceBroker.class);
        boolean expectUpdateServiceBroker = expectedOutput != null && expectedOutput.updatedServiceBroker != null;
        if (expectUpdateServiceBroker) {
            Mockito.verify(client, Mockito.times(1))
                   .updateServiceBroker(updateArgumentCaptor.capture());
            actualOutput.updatedServiceBroker = updateArgumentCaptor.getValue();
        } else {
            Mockito.verify(client, Mockito.never())
                   .updateServiceBroker(Mockito.any());
        }

        return actualOutput;
    }

    private void applyExpectedWarningMessage(String expectedWarningMessage) {
        if (expectedWarningMessage != null) {
            Mockito.verify(stepLogger)
                   .warn(expectedWarningMessage);
        }
    }

    private void validateServiceBrokers(StepOutput expectedOutput) {
        CloudServiceBroker actuallyCreatedOrUpdatedServiceBroker = context.getVariable(Variables.CREATED_OR_UPDATED_SERVICE_BROKER);
        CloudServiceBroker expectedCreatedServiceBroker = expectedOutput.createdServiceBroker;
        CloudServiceBroker expectedUpdatedServiceBroker = expectedOutput.updatedServiceBroker;

        if (Objects.nonNull(expectedCreatedServiceBroker)) {
            assertEquals(JsonUtil.toJson(expectedCreatedServiceBroker, true), JsonUtil.toJson(actuallyCreatedOrUpdatedServiceBroker, true));
        }
        if (Objects.nonNull(expectedUpdatedServiceBroker)) {
            assertEquals(JsonUtil.toJson(expectedUpdatedServiceBroker, true), JsonUtil.toJson(actuallyCreatedOrUpdatedServiceBroker, true));
        }
    }

    private static class StepInput {
        List<CloudServiceBroker> existingServiceBrokers;
        SimpleApplication application;
        String spaceGuid;
    }

    private static class StepOutput {
        CloudServiceBroker createdServiceBroker;
        CloudServiceBroker updatedServiceBroker;
    }

    static class SimpleApplication {

        String name;
        Map<String, Object> attributes;

        CloudApplicationExtended toCloudApplication() {
            return ImmutableCloudApplicationExtended.builder()
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

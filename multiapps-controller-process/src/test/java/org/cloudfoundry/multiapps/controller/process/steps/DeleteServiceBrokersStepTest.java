package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceBroker;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.test.TestUtil;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.controller.process.steps.CreateOrUpdateServiceBrokerStepTest.SimpleApplication;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;

class DeleteServiceBrokersStepTest extends SyncFlowableStepTest<DeleteServiceBrokersStep> {

    private StepInput input;

    private class DeleteServiceBrokersStepMock extends DeleteServiceBrokersStep {
        @Override
        protected List<String> getCreatedOrUpdatedServiceBrokerNames(ProcessContext context) {
            return input.serviceBrokersToCreate;
        }
    }

    public static Stream<Arguments> testExecute() {
        return Stream.of(
// @formatter:off
            // (0) One service broker  should be deleted:
            Arguments.of("delete-service-brokers-step-input-01.json", new String[] { "foo-broker", }, null, null, null),
            // (1) No service brokers to delete:
            Arguments.of("delete-service-brokers-step-input-02.json", new String[] {}, null, null, null),
            // (2) Two service brokers should be deleted:
            Arguments.of("delete-service-brokers-step-input-03.json", new String[] { "foo-broker", "bar-broker", }, null, null, null),
            // (3) One service broker should be deleted, but it doesn't exist:
            Arguments.of("delete-service-brokers-step-input-04.json", new String[] {}, null, null, null),
            // (4) A module that provides a service broker was renamed (the service broker was updated):
            Arguments.of("delete-service-brokers-step-input-05.json", new String[] {}, null, null, null),
            // (5) One service broker  should be deleted, but an exception is thrown by the client:
            Arguments.of("delete-service-brokers-step-input-01.json", new String[] {}, "Controller operation failed: 418 I'm a teapot", SLException.class, new CloudOperationException(HttpStatus.I_AM_A_TEAPOT)),
            // (6) Service broker should not be deleted and an exception should be thrown, because the user is not an admin and failsafe option is not set:
            Arguments.of("delete-service-brokers-step-input-01.json", new String[] { "foo-broker", }, "Service broker operation failed: 403 Forbidden", SLException.class, new CloudOperationException(HttpStatus.FORBIDDEN)),
            // (7) Service broker should not be deleted without an exception, because the user is not an admin and failsafe option is set:
            Arguments.of("delete-service-brokers-step-input-01.json", new String[] { "foo-broker", }, null, null, new CloudOperationException(HttpStatus.FORBIDDEN))
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(String inputLocation, String[] expectedDeletedBrokers, String expectedExceptionMessage,
                     Class<? extends Throwable> expectedExceptionClass, CloudOperationException deleteException) {
        input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputLocation, getClass()), StepInput.class);
        initializeParameters(deleteException);
        if (expectedExceptionMessage != null) {
            context.setVariable(Variables.NO_FAIL_ON_MISSING_PERMISSIONS, false);
            Throwable throwable = assertThrows(getExpectedExceptionClass(expectedExceptionClass), () -> step.execute(execution));
            assertTrue(throwable.getMessage()
                                .contains(expectedExceptionMessage));
            return;
        }
        context.setVariable(Variables.NO_FAIL_ON_MISSING_PERMISSIONS, true);

        step.execute(execution);

        assertStepFinishedSuccessfully();

        String[] deletedBrokers = captureStepOutput(expectedDeletedBrokers);

        assertArrayEquals(expectedDeletedBrokers, deletedBrokers);
    }

    private void initializeParameters(CloudOperationException deleteException) {
        loadParameters();
        prepareContext();
        prepareClient(deleteException);
    }

    private void loadParameters() {
    }

    private void prepareContext() {
        context.setVariable(Variables.APPS_TO_UNDEPLOY, toCloudApplications(input.applicationsToUndeploy));
    }

    private List<CloudApplication> toCloudApplications(List<SimpleApplication> applications) {
        return applications.stream()
                           .map(SimpleApplication::toCloudApplication)
                           .collect(Collectors.toList());
    }

    private void prepareClient(CloudOperationException deleteException) {
        Mockito.when(client.getServiceBroker(Mockito.anyString(), Mockito.eq(false)))
               .then((Answer<CloudServiceBroker>) invocation -> {
                   String serviceBrokerName = (String) invocation.getArguments()[0];
                   if (input.existingServiceBrokers.contains(serviceBrokerName)) {
                       return ImmutableCloudServiceBroker.builder()
                                                         .name(serviceBrokerName)
                                                         .build();
                   }
                   return null;
               });
        if (deleteException != null) {
            Mockito.doThrow(deleteException)
                   .when(client)
                   .deleteServiceBroker(Mockito.any());
        }
    }

    private Class<? extends Throwable> getExpectedExceptionClass(Class<? extends Throwable> expectedExceptionClass) {
        return (expectedExceptionClass != null) ? expectedExceptionClass : CloudControllerException.class;
    }

    private String[] captureStepOutput(String[] expectedDeletedBrokers) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(client, Mockito.times(expectedDeletedBrokers.length))
               .deleteServiceBroker(captor.capture());
        return captor.getAllValues()
                     .toArray(new String[0]);
    }

    private static class StepInput {
        List<String> existingServiceBrokers;
        List<SimpleApplication> applicationsToUndeploy;
        List<String> serviceBrokersToCreate;
    }

    @Override
    protected DeleteServiceBrokersStep createStep() {
        return new DeleteServiceBrokersStepMock();
    }

}

package org.cloudfoundry.multiapps.controller.process.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.controller.core.model.SupportedParameters;
import org.cloudfoundry.multiapps.controller.process.steps.CreateOrUpdateServiceBrokerStepTest.SimpleApplication;
import org.cloudfoundry.multiapps.controller.process.variables.Variables;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.CloudControllerException;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.CloudServiceBrokerException;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBroker;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceBroker;

class DeleteServiceBrokersStepTest extends SyncFlowableStepTest<DeleteServiceBrokersStep> {

    private static final String SERVICE_BROKER_NAME = "foo-broker";
    private static final String SERVICE_BROKER_NAME_2 = "bar-broker";
    private static final String APPLICATION_NAME = "foo";
    private static final String APPLICATION_NAME_2 = "bar";
    private static final String APPLICATION_NAME_3 = "foo-blue";

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
            // (1) One service broker should be deleted:
            Arguments.of(new StepInput(List.of(SERVICE_BROKER_NAME), 
                                       List.of(new SimpleApplication(APPLICATION_NAME, Map.of(SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                                              SupportedParameters.CREATE_SERVICE_BROKER, true)),
                                               new SimpleApplication(APPLICATION_NAME_2, null))), List.of(SERVICE_BROKER_NAME), null, null, null),
            // (2) No service brokers to delete:
            Arguments.of(new StepInput(Collections.emptyList(), List.of(new SimpleApplication(APPLICATION_NAME, null), new SimpleApplication(APPLICATION_NAME_2, null))),
                         Collections.emptyList(), null, null, null),
            // (3) Two service brokers should be deleted:
            Arguments.of(new StepInput(List.of(SERVICE_BROKER_NAME, SERVICE_BROKER_NAME_2),
                                       List.of(new SimpleApplication(APPLICATION_NAME, Map.of(SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                                              SupportedParameters.CREATE_SERVICE_BROKER, true)),
                                               new SimpleApplication(APPLICATION_NAME_2, Map.of(SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME_2,
                                                                                                SupportedParameters.CREATE_SERVICE_BROKER, true)))),
                         List.of(SERVICE_BROKER_NAME_2, SERVICE_BROKER_NAME), null, null, null),
            // (4) One service broker should be deleted, but it doesn't exist:
            Arguments.of(new StepInput(Collections.emptyList(),
                                       List.of(new SimpleApplication(APPLICATION_NAME, Map.of(SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                                               SupportedParameters.CREATE_SERVICE_BROKER, true)),
                                                new SimpleApplication(APPLICATION_NAME_2, null))), Collections.emptyList(), null, null, null),
            // (5) A module that provides a service broker was renamed (the service broker was updated):
            Arguments.of(new StepInput(List.of(SERVICE_BROKER_NAME), 
                                       List.of(new SimpleApplication(APPLICATION_NAME_3, Map.of(SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                                              SupportedParameters.CREATE_SERVICE_BROKER, true)),
                                               new SimpleApplication(APPLICATION_NAME_2, null)), List.of(SERVICE_BROKER_NAME)), Collections.emptyList(), null, null, null),
            // (6) One service broker  should be deleted, but an exception is thrown by the client:
            Arguments.of(new StepInput(List.of(SERVICE_BROKER_NAME), 
                                       List.of(new SimpleApplication(APPLICATION_NAME, Map.of(SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                                              SupportedParameters.CREATE_SERVICE_BROKER, true)),
                                               new SimpleApplication(APPLICATION_NAME_2, null))),
                         null, "Controller operation failed: 418 I'm a teapot", CloudControllerException.class, new CloudOperationException(HttpStatus.I_AM_A_TEAPOT)),
            // (7) Service broker should not be deleted and an exception should be thrown, because the user is not an admin and failsafe option is not set:
            Arguments.of(new StepInput(List.of(SERVICE_BROKER_NAME), 
                                       List.of(new SimpleApplication(APPLICATION_NAME, Map.of(SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                                              SupportedParameters.CREATE_SERVICE_BROKER, true)),
                                               new SimpleApplication(APPLICATION_NAME_2, null))),
                         null, "Service broker operation failed: 403 Forbidden", CloudServiceBrokerException.class, new CloudOperationException(HttpStatus.FORBIDDEN)),
            // (8) Service broker should not be deleted without an exception, because the user is not an admin and failsafe option is set:
            Arguments.of(new StepInput(List.of(SERVICE_BROKER_NAME), 
                                       List.of(new SimpleApplication(APPLICATION_NAME, Map.of(SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                                              SupportedParameters.CREATE_SERVICE_BROKER, true)),
                                               new SimpleApplication(APPLICATION_NAME_2, null))),
                         List.of(SERVICE_BROKER_NAME), null, null, new CloudOperationException(HttpStatus.FORBIDDEN)),
            // (9) Service broker call returns 502 bad gateway error:
            Arguments.of(new StepInput(List.of(SERVICE_BROKER_NAME), 
                                       List.of(new SimpleApplication(APPLICATION_NAME, Map.of(SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                                              SupportedParameters.CREATE_SERVICE_BROKER, true)),
                                               new SimpleApplication(APPLICATION_NAME_2, null))),
                         null, "Service broker operation failed: 502 Bad Gateway", CloudServiceBrokerException.class, new CloudOperationException(HttpStatus.BAD_GATEWAY)),
            // (10) Service broker call returns 409 conflict error:
            Arguments.of(new StepInput(List.of(SERVICE_BROKER_NAME), 
                                       List.of(new SimpleApplication(APPLICATION_NAME, Map.of(SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                                              SupportedParameters.CREATE_SERVICE_BROKER, true)),
                                               new SimpleApplication(APPLICATION_NAME_2, null))),
                         null, "Service broker operation failed: 409 Conflict", CloudServiceBrokerException.class, new CloudOperationException(HttpStatus.CONFLICT)),
            // (11) Multiple service brokers triggered to be deleted:
            Arguments.of(new StepInput(List.of(SERVICE_BROKER_NAME, SERVICE_BROKER_NAME_2),
                                       List.of(new SimpleApplication(APPLICATION_NAME, Map.of(SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME,
                                                                                              SupportedParameters.CREATE_SERVICE_BROKER, true)),
                                               new SimpleApplication(APPLICATION_NAME_2, Map.of(SupportedParameters.SERVICE_BROKER_NAME, SERVICE_BROKER_NAME_2,
                                                                                                SupportedParameters.CREATE_SERVICE_BROKER, true))),
                                       Map.of(SERVICE_BROKER_NAME, "1", SERVICE_BROKER_NAME_2, "2")),
                         List.of(SERVICE_BROKER_NAME, SERVICE_BROKER_NAME_2), null, null, null)
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExecute(StepInput stepInput, List<String> expectedDeletedBrokers, String expectedExceptionMessage,
                     Class<? extends Exception> expectedExceptionClass, CloudOperationException deleteException) {
        input = stepInput;
        initializeParameters(deleteException);
        if (expectedExceptionMessage != null) {
            context.setVariable(Variables.NO_FAIL_ON_MISSING_PERMISSIONS, false);
            Exception exception = assertThrows(SLException.class, () -> step.execute(execution));
            assertEquals(expectedExceptionClass, exception.getCause()
                                                          .getClass());
            assertTrue(exception.getMessage()
                                .contains(expectedExceptionMessage),
                       MessageFormat.format("Expected to contain \"{0}\" but was \"{1}\"", expectedExceptionMessage,
                                            exception.getMessage()));
            return;
        }
        context.setVariable(Variables.NO_FAIL_ON_MISSING_PERMISSIONS, true);

        step.execute(execution);

        assertStepPhaseStatus();
        assertDeletedBrokers(expectedDeletedBrokers);
    }

    private void initializeParameters(CloudOperationException deleteException) {
        prepareContext();
        prepareClient(deleteException);
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

        input.serviceBrokerNamesJobIds.entrySet()
                                      .forEach(serviceBrokerNameJobId -> Mockito.when(client.deleteServiceBroker(serviceBrokerNameJobId.getKey()))
                                                                                .thenReturn(serviceBrokerNameJobId.getValue()));
    }

    private void assertStepPhaseStatus() {
        if (input.serviceBrokerNamesJobIds.isEmpty()) {
            assertStepFinishedSuccessfully();
            return;
        }
        assertEquals(StepPhase.POLL.toString(), getExecutionStatus());
    }

    private List<String> captureStepOutput(List<String> expectedDeletedBrokers) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(client, Mockito.times(expectedDeletedBrokers.size()))
               .deleteServiceBroker(captor.capture());
        return captor.getAllValues();
    }

    private void assertDeletedBrokers(List<String> expectedDeletedBrokers) {
        List<String> deletedBrokers = captureStepOutput(expectedDeletedBrokers);
        assertTrue(CollectionUtils.isEqualCollection(expectedDeletedBrokers, deletedBrokers),
                   MessageFormat.format("Expected to contain \"{0}\" but was \"{1}\"", expectedDeletedBrokers, deletedBrokers));
        if (!input.serviceBrokerNamesJobIds.isEmpty()) {
            assertEquals(input.serviceBrokerNamesJobIds, context.getVariable(Variables.SERVICE_BROKER_NAMES_JOB_IDS));
        }
    }

    private static class StepInput {
        List<String> existingServiceBrokers;
        List<SimpleApplication> applicationsToUndeploy;
        List<String> serviceBrokersToCreate;
        Map<String, String> serviceBrokerNamesJobIds;

        StepInput(List<String> existingServiceBrokers, List<SimpleApplication> applicationsToUndeploy,
                  Map<String, String> serviceBrokerNamesJobIds) {
            this(existingServiceBrokers, applicationsToUndeploy);
            this.serviceBrokerNamesJobIds = serviceBrokerNamesJobIds;
        }

        StepInput(List<String> existingServiceBrokers, List<SimpleApplication> applicationsToUndeploy) {
            this(existingServiceBrokers, applicationsToUndeploy, Collections.emptyList());
        }

        StepInput(List<String> existingServiceBrokers, List<SimpleApplication> applicationsToUndeploy,
                  List<String> serviceBrokersToCreate) {
            this.existingServiceBrokers = existingServiceBrokers;
            this.applicationsToUndeploy = applicationsToUndeploy;
            this.serviceBrokersToCreate = serviceBrokersToCreate;
            this.serviceBrokerNamesJobIds = Collections.emptyMap();
        }

    }

    @Override
    protected DeleteServiceBrokersStep createStep() {
        return new DeleteServiceBrokersStepMock();
    }

}

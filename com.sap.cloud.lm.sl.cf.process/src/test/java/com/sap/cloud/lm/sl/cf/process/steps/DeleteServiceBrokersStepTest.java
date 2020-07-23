package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceBroker;
import org.cloudfoundry.multiapps.common.SLException;
import org.cloudfoundry.multiapps.common.util.JsonUtil;
import org.cloudfoundry.multiapps.common.util.TestUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.process.steps.CreateOrUpdateServiceBrokerStepTest.SimpleApplication;
import com.sap.cloud.lm.sl.cf.process.variables.Variables;

@RunWith(Parameterized.class)
public class DeleteServiceBrokersStepTest extends SyncFlowableStepTest<DeleteServiceBrokersStep> {

    private final String inputLocation;
    private final String[] expectedDeletedBrokers;
    private final String expectedExceptionMessage;
    private final Class<? extends Throwable> expectedExceptionClass;

    private final CloudOperationException deleteException;

    private StepInput input;

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    private class DeleteServiceBrokersStepMock extends DeleteServiceBrokersStep {
        @Override
        protected List<String> getCreatedOrUpdatedServiceBrokerNames(ProcessContext context) {
            return input.serviceBrokersToCreate;
        }
    }

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) One service broker  should be deleted:
            {
                "delete-service-brokers-step-input-01.json", new String[] { "foo-broker", }, null, null, null
            },
            // (1) No service brokers to delete:
            {
                "delete-service-brokers-step-input-02.json", new String[] {}, null, null, null
            },
            // (2) Two service brokers should be deleted:
            {
                "delete-service-brokers-step-input-03.json", new String[] { "foo-broker", "bar-broker", }, null, null, null
            },
            // (3) One service broker should be deleted, but it doesn't exist:
            {
                "delete-service-brokers-step-input-04.json", new String[] {}, null, null, null
            },
            // (4) A module that provides a service broker was renamed (the service broker was updated):
            {
                "delete-service-brokers-step-input-05.json", new String[] {}, null, null, null
            },
            // (5) One service broker  should be deleted, but an exception is thrown by the client:
            {
                "delete-service-brokers-step-input-01.json", new String[] {}, "Controller operation failed: 418 I'm a teapot", SLException.class, new CloudOperationException(HttpStatus.I_AM_A_TEAPOT),
            },
            // (6) Service broker should not be deleted and an exception should be thrown, because the user is not an admin and failsafe option is not set:
            {
                "delete-service-brokers-step-input-01.json", new String[] { "foo-broker", }, "Service broker operation failed: 403 Forbidden", SLException.class, new CloudOperationException(HttpStatus.FORBIDDEN),
            },
            // (7) Service broker should not be deleted without an exception, because the user is not an admin and failsafe option is set:
            {
                "delete-service-brokers-step-input-01.json", new String[] { "foo-broker", }, null, null, new CloudOperationException(HttpStatus.FORBIDDEN),
            },
// @formatter:on
        });
    }

    public DeleteServiceBrokersStepTest(String inputLocation, String[] expectedDeletedBrokers, String expectedExceptionMessage,
                                        Class<? extends Throwable> expectedExceptionClass, CloudOperationException deleteException) {
        this.expectedDeletedBrokers = expectedDeletedBrokers;
        this.expectedExceptionMessage = expectedExceptionMessage;
        this.expectedExceptionClass = (expectedExceptionClass != null) ? expectedExceptionClass : CloudControllerException.class;
        this.inputLocation = inputLocation;
        this.deleteException = deleteException;
    }

    @Before
    public void setUp() throws Exception {
        loadParameters();
        prepareContext();
        prepareClient();
    }

    @Test
    public void testExecute() {
        step.execute(execution);

        assertStepFinishedSuccessfully();

        String[] deletedBrokers = captureStepOutput();

        assertArrayEquals(expectedDeletedBrokers, deletedBrokers);
    }

    private void loadParameters() {
        boolean shouldSucceed = true;
        if (expectedExceptionMessage != null) {
            expectedException.expectMessage(expectedExceptionMessage);
            expectedException.expect(expectedExceptionClass);
            shouldSucceed = false;
        }
        context.setVariable(Variables.NO_FAIL_ON_MISSING_PERMISSIONS, shouldSucceed);
        input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputLocation, getClass()), StepInput.class);
    }

    private void prepareContext() {
        context.setVariable(Variables.APPS_TO_UNDEPLOY, toCloudApplications(input.applicationsToUndeploy));
    }

    private List<CloudApplication> toCloudApplications(List<SimpleApplication> applications) {
        return applications.stream()
                           .map(SimpleApplication::toCloudApplication)
                           .collect(Collectors.toList());
    }

    private void prepareClient() {
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

    private String[] captureStepOutput() {
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

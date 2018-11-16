package com.sap.cloud.lm.sl.cf.process.steps;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.CloudControllerException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpStatus;

import com.sap.cloud.lm.sl.cf.client.lib.domain.CloudServiceBrokerExtended;
import com.sap.cloud.lm.sl.cf.process.Constants;
import com.sap.cloud.lm.sl.cf.process.steps.CreateOrUpdateServiceBrokersStepTest.SimpleApplication;
import com.sap.cloud.lm.sl.common.util.JsonUtil;
import com.sap.cloud.lm.sl.common.util.TestUtil;

@RunWith(Parameterized.class)
public class DeleteServiceBrokersStepTest extends SyncFlowableStepTest<DeleteServiceBrokersStep> {

    private final String inputLocation;
    private final String[] expectedDeletedBrokers;
    private final String expectedExceptionMessage;

    private CloudOperationException deleteException;

    private StepInput input;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Parameters
    public static Iterable<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
// @formatter:off
            // (0) One service broker  should be deleted:
            {
                "delete-service-brokers-step-input-01.json", new String[] { "foo-broker", }, null, null,
            },
            // (1) No service brokers to delete:
            {
                "delete-service-brokers-step-input-02.json", new String[] {}, null, null,
            },
            // (2) Two service brokers should be deleted:
            {
                "delete-service-brokers-step-input-03.json", new String[] { "foo-broker", "bar-broker", }, null, null,
            },
            // (3) One service broker should be deleted, but it doesn't exist:
            {
                "delete-service-brokers-step-input-04.json", new String[] {}, null, null,
            },
            // (4) A module that provides a service broker was renamed (the service broker was updated):
            {
                "delete-service-brokers-step-input-05.json", new String[] {}, null, null,
            },
            // (5) One service broker  should be deleted, but an exception is thrown by the client:
            {
                "delete-service-brokers-step-input-01.json", new String[] {}, "Controller operation failed: 418 I'm a teapot", new CloudOperationException(HttpStatus.I_AM_A_TEAPOT),
            },
            // (6) Service broker should not be deleted and an exception should be thrown, because the user is not an admin and failsafe option is not set:
            {
                "delete-service-brokers-step-input-01.json", new String[] { "foo-broker", }, "Controller operation failed: 403 Forbidden", new CloudOperationException(HttpStatus.FORBIDDEN),
            },
            // (7) Service broker should not be deleted without an exception, because the user is not an admin and failsafe option is set:
            {
                "delete-service-brokers-step-input-01.json", new String[] { "foo-broker", }, null, new CloudOperationException(HttpStatus.FORBIDDEN),
            },
// @formatter:on
        });
    }

    public DeleteServiceBrokersStepTest(String inputLocation, String[] expectedDeletedBrokers, String expectedExceptionMessage,
        CloudOperationException deleteException) {
        this.expectedDeletedBrokers = expectedDeletedBrokers;
        this.expectedExceptionMessage = expectedExceptionMessage;
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
    public void testExecute() throws Exception {
        step.execute(context);

        assertStepFinishedSuccessfully();

        String[] deletedBrokers = captureStepOutput();

        assertArrayEquals(expectedDeletedBrokers, deletedBrokers);
    }

    private void loadParameters() throws Exception {
        boolean shouldSucceed = true;
        if (expectedExceptionMessage != null) {
            expectedException.expectMessage(expectedExceptionMessage);
            expectedException.expect(CloudControllerException.class);
            shouldSucceed = false;
        }
        context.setVariable(Constants.PARAM_NO_FAIL_ON_MISSING_PERMISSIONS, shouldSucceed);
        input = JsonUtil.fromJson(TestUtil.getResourceAsString(inputLocation, getClass()), StepInput.class);
    }

    private void prepareContext() {
        StepsUtil.setAppsToUndeploy(context, toCloudApplications(input.applicationsToUndeploy));
        StepsUtil.setServiceBrokersToCreate(context, toCloudServiceBrokers(input.serviceBrokersToCreate));
    }

    private List<CloudServiceBrokerExtended> toCloudServiceBrokers(List<String> serviceBrokerNames) {
        return serviceBrokerNames.stream()
            .map((serviceBrokerName) -> toCloudServiceBroker(serviceBrokerName))
            .collect(Collectors.toList());
    }

    private CloudServiceBrokerExtended toCloudServiceBroker(String serviceBrokerName) {
        return new CloudServiceBrokerExtended(null, serviceBrokerName, null, null, null, null);
    }

    private List<CloudApplication> toCloudApplications(List<SimpleApplication> applications) {
        return applications.stream()
            .map((application) -> application.toCloudApplication())
            .collect(Collectors.toList());
    }

    private void prepareClient() {
        Mockito.when(client.getServiceBroker(Mockito.anyString(), Mockito.eq(false)))
            .then(new Answer<CloudServiceBroker>() {
                @Override
                public CloudServiceBroker answer(InvocationOnMock invocation) {
                    String serviceBrokerName = (String) invocation.getArguments()[0];
                    if (input.existingServiceBrokers.contains(serviceBrokerName)) {
                        return new CloudServiceBroker(null, serviceBrokerName, null, null, null);
                    }
                    return null;
                }
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
        return new DeleteServiceBrokersStep();
    }

}
